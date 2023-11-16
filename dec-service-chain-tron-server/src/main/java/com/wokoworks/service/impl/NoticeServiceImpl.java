package com.wokoworks.service.impl;

import com.alibaba.fastjson.JSON;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.wokoworks.chain.RpcClient;
import com.wokoworks.chain.notice.client.WokoNoticeClient;
import com.wokoworks.chain.notice.common.codes.NoticeCode;
import com.wokoworks.chain.notice.common.params.NoticeParam;
import com.wokoworks.chain.vo.BlockTransaction;
import com.wokoworks.config.ChainProperties;
import com.wokoworks.framework.commons.data.CallValue;
import com.wokoworks.mq.MsgProducer;
import com.wokoworks.repository.SubscribeAddressRepository;
import com.wokoworks.repository.SubscribeApplicationAddressRepository;
import com.wokoworks.service.CacheService;
import com.wokoworks.service.NoticeService;
import com.wokoworks.utils.FileType;
import com.wokoworks.utils.NftUtils;
import com.wokoworks.vo.SubscribeAddress;
import com.wokoworks.vo.SubscribeApplicationAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: 飞
 * @Date: 2021/10/13 10:54
 */
@Slf4j
@Service
public class NoticeServiceImpl implements NoticeService {

    @Autowired
    private SubscribeAddressRepository subscribeAddressRepository;
    @Autowired
    private SubscribeApplicationAddressRepository subscribeApplicationAddressRepository;
    @Autowired
    private WokoNoticeClient wokoNoticeClient;
    @Autowired
    private ChainProperties chainProperties;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private RpcClient rpcClient;
    @Autowired
    private MsgProducer msgProducer;


    @Override
    public boolean notice(BlockTransaction blockTransaction) {

        final List<BlockTransaction.Transaction> transactionList = blockTransaction.getTransactionList();
        if (transactionList.isEmpty()) {
            return true;
        }

        /**
         * 查询订阅地址(匹配交易)
         */
        final List<SubscribeAddress> subscribeAddressList = prefetchAddress(transactionList);
        if (subscribeAddressList.isEmpty()) {
            // 没有匹配订阅地址
            return true;
        }
        Set<String> subscribeAddressSet = subscribeAddressList.stream().map(SubscribeAddress::getAddress).collect(Collectors.toSet());


        /**
         * 匹配出所有有用户订阅的地址
         */
        List<BlockTransaction.Transaction> userTransactionList = transactionList.stream().filter((transaction) ->
                (!Strings.isNullOrEmpty(transaction.getFromAddress()) && subscribeAddressSet.contains(transaction.getFromAddress()))
                        || (!Strings.isNullOrEmpty(transaction.getToAddress()) && subscribeAddressSet.contains(transaction.getToAddress()))
        ).collect(Collectors.toList());

        /**
         * 填充解析用户交易里的合约信息
         */
        rpcClient.fillContractInfo(userTransactionList);
        log.debug("<==解析到完整交易信息:{}", userTransactionList);

        BlockTransaction blockTransactionMq = new BlockTransaction();
        blockTransactionMq.setBlockNumber(blockTransaction.getBlockNumber());
        blockTransactionMq.setBlockHash(blockTransaction.getBlockHash());
        blockTransactionMq.setBlockDt(blockTransaction.getBlockDt());
        blockTransactionMq.setTransactionList(userTransactionList);
        msgProducer.sendAssetService(blockTransactionMq);

        // 2 预先查询
        // 地址map
        final Map<String, SubscribeAddress> subscribeAddressMap = new HashMap<>();
        // 地址id集合
        final Set<Integer> subscribeAddressIdSet = new HashSet<>();
        for (SubscribeAddress item : subscribeAddressList) {
            subscribeAddressMap.put(item.getAddress().toLowerCase(), item);
            subscribeAddressIdSet.add(item.getId());
        }

        // 地址id订阅了哪些appNum key:链地址id , value:订阅的项目集合
        Map<Integer, List<SubscribeApplicationAddress>> subscribeApplicationAddressMap = getSubscribeApplicationAddress(subscribeAddressIdSet);
        if (subscribeApplicationAddressMap == null || subscribeApplicationAddressMap.isEmpty()) {
            // 没有匹配项目订阅
            log.debug("没有匹配的订阅地址, blockNumber:{}", blockTransaction.getBlockNumber());
            return true;
        }


        // 3 组装
        // 交易信息
        List<Map<String, Object>> transactionInputList = new ArrayList<>();
        // 项目交易关系
        Map<String, List<Integer>> appTransactionMap = new HashMap<>();

        for (BlockTransaction.Transaction transaction : transactionList) {

            final String fromAddress = transaction.getFromAddress().toLowerCase();
            final String toAddress = transaction.getToAddress().toLowerCase();

            //转出
            if (subscribeAddressMap.containsKey(fromAddress)) {
                final SubscribeAddress subscribeAddress = subscribeAddressMap.get(fromAddress);
                // 哪些应用订阅了这个地址
                final List<SubscribeApplicationAddress> list = subscribeApplicationAddressMap.get(subscribeAddress.getId());

                if (list != null && !list.isEmpty()) {
                    setNoticeParam(transactionInputList, appTransactionMap, transaction, list, TransactionType.OUTLAY);
                } else {
                    log.warn("<== 数据有些异常或者并发情况 fromAddress:{} 没有关联项目", fromAddress);
                }
            }
            //转入
            if (subscribeAddressMap.containsKey(toAddress)) {
                final SubscribeAddress subscribeAddress = subscribeAddressMap.get(toAddress);
                // 哪些应用订阅了这个地址
                final List<SubscribeApplicationAddress> list = subscribeApplicationAddressMap.get(subscribeAddress.getId());

                if (list != null && !list.isEmpty()) {
                    setNoticeParam(transactionInputList, appTransactionMap, transaction, list, TransactionType.INCOME);
                } else {
                    log.warn("<== 数据有些异常或者并发情况 toAddress:{} 没有关联项目", toAddress);
                }
            }
        }


        NoticeParam.CreateNoticeInput createNoticeInput = new NoticeParam.CreateNoticeInput();
        createNoticeInput.setChain(chainProperties.getChainCode());
        createNoticeInput.setBlockNumber(blockTransaction.getBlockNumber());
        createNoticeInput.setBlockHash(blockTransaction.getBlockHash());
        createNoticeInput.setBlockDt(blockTransaction.getBlockDt());
        createNoticeInput.setAppTransactionMap(appTransactionMap);
        createNoticeInput.setTransactionList(transactionInputList);
        createNoticeInput.setChainCode("tron");
        CallValue<Void, NoticeCode.CreateNoticeCode> noticeCallValue;

        try {
            log.debug("==>发送notice交易createNoticeInput:{}", JSON.toJSONString(createNoticeInput));
            noticeCallValue = wokoNoticeClient.createNotice(createNoticeInput);
        } catch (Exception ex) {
            log.warn("<== 调用通知服务异常 blockNumber:{}, blockHash:{}, errorMsg:{}", createNoticeInput.getBlockNumber(), createNoticeInput.getBlockHash(), ex.getMessage());
            return false;
        }
        if (noticeCallValue.hasError()) {
            log.error("<== 调用通知服务出错 error:{}, blockNumber:{}, blockHash:{}", noticeCallValue.getError(), createNoticeInput.getBlockNumber(), createNoticeInput.getBlockHash());
            return false;
        }

        return true;

    }

//    private List<BlockTransaction.Transaction> convert1155Transaction(List<BlockTransaction.Transaction> userTransactionList) {
//        List<BlockTransaction.Transaction> list = new ArrayList<>();
//        for (BlockTransaction.Transaction transaction : userTransactionList) {
//            if (transaction.getEip() .equals("1155")) {
//                final Map<BigInteger, BigInteger> nftMap = transaction.getNftMap();
//                nftMap.forEach((tokenId, amount)->{
//                    final BlockTransaction.Transaction transaction_ = new BlockTransaction.Transaction();
//                    BeanUtils.copyProperties(transaction_, transaction);
//                    transaction_.setValue(tokenId);
//                    transaction_.setAmount(new BigDecimal(amount));
//                    list.add(transaction_);
//                });
//            } else {
//                list.add(transaction);
//            }
//        }
//        return list;
//    }


    private List<SubscribeAddress> prefetchAddress(List<BlockTransaction.Transaction> transactionList) {
        Set<String> addressSet = new HashSet<>();
        for (BlockTransaction.Transaction item : transactionList) {
            addressSet.add(item.getFromAddress());
            addressSet.add(item.getToAddress());
        }
        return subscribeAddressRepository.selectInAddress(addressSet);
    }


    private Map<Integer, List<SubscribeApplicationAddress>> getSubscribeApplicationAddress(Set<Integer> subscribeAddressIdSet) {
        List<SubscribeApplicationAddress> subscribeApplicationAddressList = subscribeApplicationAddressRepository.selectInSubscribeAddressIds(subscribeAddressIdSet);

        //按地址id分组
        Map<Integer, List<SubscribeApplicationAddress>> subscribeApplicationAddressMap = new HashMap<>();
        for (SubscribeApplicationAddress item : subscribeApplicationAddressList) {
            List<SubscribeApplicationAddress> list;
            if (subscribeApplicationAddressMap.containsKey(item.getSubscribeAddressId())) {
                list = subscribeApplicationAddressMap.get(item.getSubscribeAddressId());
            } else {
                list = new ArrayList<>();
                subscribeApplicationAddressMap.put(item.getSubscribeAddressId(), list);
            }
            list.add(item);
        }
        return subscribeApplicationAddressMap;
    }


    private void setNoticeParam(List<Map<String, Object>> transactionInputList, Map<String, List<Integer>> appTransactionMap, BlockTransaction.Transaction transaction,
                                List<SubscribeApplicationAddress> subscribeApplicationAddressList, TransactionType type) {


        String address;
        String counterpartyAddress;
        switch (type) {
            case INCOME:
                address = transaction.getToAddress();
                counterpartyAddress = transaction.getFromAddress();
                break;
            case OUTLAY:
                address = transaction.getFromAddress();
                counterpartyAddress = transaction.getToAddress();
                break;
            default:
                return;
        }
        Map<String, Object> transactionMap = new HashMap<>();
        transactionMap.put(TransactionMapKey.unit, transaction.getUnit());
        transactionMap.put(TransactionMapKey.contractAddress, transaction.getContractAddress());
        transactionMap.put(TransactionMapKey.transactionHash, transaction.getTransactionHash());
        transactionMap.put(TransactionMapKey.address, address);
        transactionMap.put(TransactionMapKey.counterpartyAddress, counterpartyAddress);
        transactionMap.put(TransactionMapKey.amount, transaction.getAmount().stripTrailingZeros().toPlainString());
        transactionMap.put(TransactionMapKey.type, type.value);
        transactionMap.put(TransactionMapKey.methodId, transaction.getMethodId());
        transactionMap.put(TransactionMapKey.netUsage, transaction.getNetUsage());
        transactionMap.put(TransactionMapKey.netFee, transaction.getNetFee().toPlainString());
        transactionMap.put(TransactionMapKey.energyFee, transaction.getEnergyFee().toPlainString());
        transactionMap.put(TransactionMapKey.energyUsage, transaction.getEnergyUsage());
        transactionMap.put(TransactionMapKey.energyUsageTotal, transaction.getEnergyUsageTotal());
        transactionMap.put(TransactionMapKey.fee, transaction.getFee());
        transactionMap.put(TransactionMapKey.eip, transaction.getEip());

        // 额外添加内容
        transactionMap.put(TransactionMapKey.crossChainTxnCheck, transaction.getCrossChainTxnCheck());
        transactionMap.put(TransactionMapKey.sourceChainName, transaction.getSourceChainName());
        transactionMap.put(TransactionMapKey.targetChainName, transaction.getTargetChainName());
        transactionMap.put(TransactionMapKey.sourceChainId, transaction.getSourceChainId());
        transactionMap.put(TransactionMapKey.targetChainId, transaction.getTargetChainId());

        //nft
        if (transaction.getEip().equals("721") || transaction.getEip().equals("1155")) {
            transactionMap.put(TransactionMapKey.nft, true);
            transactionMap.put(TransactionMapKey.tokenId, transaction.getValue());
            transactionMap.put(TransactionMapKey.tokenUri, transaction.getTokenUri());
            NftUtils.NftInfo nftInfo = transaction.getNftInfo();
            if (nftInfo != null) {
                transactionMap.put(TransactionMapKey.file, nftInfo.getInnerFile());
                transactionMap.put(TransactionMapKey.fileType, nftInfo.getInnerFileType().value);
                transactionMap.put(TransactionMapKey.nftJson, nftInfo.getUriContent());
                transactionMap.put(TransactionMapKey.nftName, transaction.getName());
                transactionMap.put(TransactionMapKey.smallImage, nftInfo.getSmallImage());
            } else {
                transactionMap.put(TransactionMapKey.file, "");
                transactionMap.put(TransactionMapKey.fileType, FileType.IMAGE.value);
                transactionMap.put(TransactionMapKey.nftJson, "");
                transactionMap.put(TransactionMapKey.nftName, "");
                transactionMap.put(TransactionMapKey.smallImage, "");
            }
        }


        int transactionIndex = transactionInputList.size();
        transactionInputList.add(transactionMap);

        for (SubscribeApplicationAddress applicationAddress : subscribeApplicationAddressList) {
            final String appNum = applicationAddress.getAppNum();
            List<Integer> list;
            if (appTransactionMap.containsKey(appNum)) {
                list = appTransactionMap.get(appNum);
            } else {
                list = new ArrayList<>();
                appTransactionMap.put(appNum, list);
            }
            list.add(transactionIndex);
        }
    }


    public enum TransactionType {
        INCOME("income", "入账"),
        OUTLAY("outlay", "出账");

        public final String value;
        public final String remark;

        TransactionType(String value, String remark) {
            this.value = value;
            this.remark = remark;
        }
    }


    public interface TransactionMapKey {
        String unit = "unit";
        String contractAddress = "contractAddress";
        String transactionHash = "transactionHash";
        String address = "address";
        String counterpartyAddress = "counterpartyAddress";
        String amount = "amount";
        String type = "type";
        String methodId = "methodId";
        String en = "gasPrice";
        String gas = "gasLimit";
        String netUsage = "netUsage";
        String netFee = "netFee";

        String energyFee = "energyFee";
        String energyUsageTotal = "energyUsageTotal";
        String energyUsage = "energyUsage";
        String fee = "tronFee";

        String crossChainTxnCheck = "crossChainTxnCheck";
        String sourceChainName = "sourceChainName";
        String targetChainName = "targetChainName";
        String sourceChainId = "sourceChainId";
        String targetChainId = "targetChainId";

        //nft
        String eip = "eip";
        String tokenId = "tokenId";
        String tokenUri = "tokenUri";
        String file = "file";
        String fileType = "fileType";
        String nft = "nft";
        String nftJson = "nftJson";
        String nftName = "nftName";
        String smallImage = "smallImage";
    }

}
