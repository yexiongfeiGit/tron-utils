package com.wokoworks.chain;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.wokoworks.chain.enums.MainnetChainEnum;
import com.wokoworks.chain.enums.TestnetChainEnum;
import com.wokoworks.chain.helper.ChainIdChecker;
import com.wokoworks.chain.helper.ChainServerHelper;
import com.wokoworks.chain.vo.ContractToken.Status;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

import java.util.*;

import com.wokoworks.chain.vo.BlockTransaction;
import com.wokoworks.chain.vo.ContractToken;
import com.wokoworks.chain.vo.NewBlockHead;
import com.wokoworks.config.ChainProperties;
import com.wokoworks.config.Web3NodeConfig;
import com.wokoworks.service.CacheService;
import com.wokoworks.utils.ByteUtils;
import com.wokoworks.utils.NftUtils;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.tron.trident.abi.FunctionEncoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.*;
import org.tron.trident.abi.datatypes.generated.Bytes4;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.api.GrpcAPI;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Base58Check;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static com.wokoworks.chain.constant.CrossConstants.FX_BRIDGE_TRON;
import static com.wokoworks.chain.constant.CrossConstants.TRON_BRIDGE_FX;

/**
 * @Author: 飞
 * @Date: 2021/10/8 14:07
 */
@Slf4j
@Data
@Configuration
public class RpcClient {

    @Autowired
    private Web3NodeConfig web3NodeConfig;

    @Autowired
    private ChainProperties chainProperties;

    private List<ApiWrapper> clients = new ArrayList<>();

    private static final Set<String> SCAN_TYPES = Sets.newHashSet("8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925", "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");


    public void init() {
        // clear
        clients.clear();
        // re-construct
        List<Web3NodeConfig.NodeInfo> curChainNodes = web3NodeConfig.getCurChainNodes();
        if (null == curChainNodes) {
            log.debug("skip empty rpc");
            return;
        }
        log.info("<== 更新rpc地址:{}", JSON.toJSONString(curChainNodes, true));
        curChainNodes.forEach(node -> {
            String grpcFullPath = node.getGrpcPath() + ":" + node.getGrpcPort();
            clients.add(
                    node.isIndirectly()
                            ? new ApiWrapper(grpcFullPath, grpcFullPath, "", node.getNodeName(), node.isSsl(), web3NodeConfig.getGtwyKey())
                            : new ApiWrapper(grpcFullPath, grpcFullPath, ""));
        });
    }

    @Autowired
    private CacheService cacheService;

    private ApiWrapper getClient() {
        return clients.get(new Random().nextInt(clients.size()));
    }


    public NewBlockHead getBlockHeader() {
        Response.BlockExtention block = getClient().blockingStub.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
        Chain.BlockHeader.raw rawData = block.getBlockHeader().getRawData();
        NewBlockHead newBlockHead = new NewBlockHead();
        newBlockHead.setBlockHash(Hex.toHexString(block.getBlockid().toByteArray()));
        newBlockHead.setBlockNumber(rawData.getNumber());
        newBlockHead.setParentHash(Hex.toHexString(rawData.getParentHash().toByteArray()));
        newBlockHead.setBlockDt(rawData.getTimestamp());
        return newBlockHead;
    }


    public NewBlockHead getBlockHeaderByBlockNumber(long blockBum) {
        GrpcAPI.NumberMessage numberMessage = GrpcAPI.NumberMessage.newBuilder().setNum(blockBum).build();
        Response.BlockExtention block = getClient().blockingStub.getBlockByNum2(numberMessage);
        Chain.BlockHeader.raw rawData = block.getBlockHeader().getRawData();
        NewBlockHead newBlockHead = new NewBlockHead();
        newBlockHead.setBlockHash(Hex.toHexString(block.getBlockid().toByteArray()));
        newBlockHead.setBlockNumber(rawData.getNumber());
        newBlockHead.setParentHash(Hex.toHexString(rawData.getParentHash().toByteArray()));
        newBlockHead.setBlockDt(rawData.getTimestamp());
        return newBlockHead;
    }


    /**
     * 获取合约精度
     *
     * @param contractAddress
     * @return
     */
    public BigInteger getDecimal(String contractAddress) {
        return getDecimal(ByteString.copyFrom(Base58Check.base58ToBytes(contractAddress)));
    }


    private BigInteger getDecimal(ByteString contractAddress) {
        Function functionSymbol = new Function("decimals", new ArrayList<>(), ImmutableList.of(TypeReference.create(Uint.class)));
        String dataSymbol = FunctionEncoder.encode(functionSymbol);
        final ByteString bytesSymbol = ByteString.copyFrom(Hex.decode(dataSymbol));
        Contract.TriggerSmartContract requestSymbol = Contract.TriggerSmartContract.newBuilder()
                .setContractAddress(contractAddress)
                .setData(bytesSymbol).build();
        final Response.TransactionExtention transactionExtentionSymbol = getClient().blockingStub.triggerConstantContract(requestSymbol);
        List<ByteString> constantResultList = transactionExtentionSymbol.getConstantResultList();
        if (constantResultList.isEmpty()) {
            log.warn("查询合约精度constantResultList为空:{}", contractAddress);
            return null;
        }

        ByteString bytes = constantResultList.get(0);
        if (bytes.isEmpty()) {
            log.warn("查询合约精度bytes为空:{}", contractAddress);
            return null;
        }
        return new BigInteger(bytes.toByteArray());
    }


    /**
     * 查询trc10的货币单位
     *
     * @param assetId
     * @return
     */
    public String getTrc10Symbol(String assetId) {
        try {
            GrpcAPI.BytesMessage bytesMessage = GrpcAPI.BytesMessage.newBuilder().setValue(ByteString.copyFrom(assetId.getBytes())).build();
            Contract.AssetIssueContract assetIssueById = getClient().blockingStub.getAssetIssueById(bytesMessage);
            return assetIssueById.getAbbr().toStringUtf8();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }

    }


    /**
     * 查询合约币种单位
     *
     * @param contractAddress
     * @return
     */
    public String getTrc20Symbol(String contractAddress) {
        return getTrc20Symbol(ByteString.copyFrom(Base58Check.base58ToBytes(contractAddress)));
    }


    private String getTrc20Symbol(ByteString contractAddress) {

        Function functionSymbol = new Function("symbol", new ArrayList<>(), ImmutableList.of(TypeReference.create(Uint.class)));
        String dataSymbol = FunctionEncoder.encode(functionSymbol);
        final ByteString bytesSymbol = ByteString.copyFrom(Hex.decode(dataSymbol));
        Contract.TriggerSmartContract requestSymbol = Contract.TriggerSmartContract.newBuilder()
                .setContractAddress(contractAddress)
                .setData(bytesSymbol).build();
        final Response.TransactionExtention transactionExtentionSymbol = getClient().blockingStub.triggerConstantContract(requestSymbol);
        if (transactionExtentionSymbol.getConstantResultList().isEmpty()) {
            return null;
        }

        final byte[] resultBytesSymbol = transactionExtentionSymbol.getConstantResult(0).toByteArray();
        List<Byte> byteList = new ArrayList<>();
        for (int i = 0; i < resultBytesSymbol.length; i++) {
            if (resultBytesSymbol[i] != 0) {
                byteList.add(resultBytesSymbol[i]);
            }
        }

        byte[] bys = new byte[byteList.size()];
        for (int i = 0; i < bys.length; i++) {
            bys[i] = byteList.get(i);
        }
        return new String(bys).trim();
    }


    @SneakyThrows
    public List<BlockTransaction.Transaction> getTRXTransaction(String hash) {

        ByteString byteString = ByteString.copyFrom(Hex.decode(hash));
        GrpcAPI.BytesMessage bytesMessage = GrpcAPI.BytesMessage.newBuilder().setValue(byteString).build();

        final Chain.Transaction transaction = getClient().blockingStub.getTransactionById(bytesMessage);

        List<BlockTransaction.Transaction> transactionList = new ArrayList<>();
        for (Chain.Transaction.Contract contract : transaction.getRawData().getContractList()) {

            // String typeUrl = contract.getParameter().getTypeUrl();
            Chain.Transaction.Contract.ContractType type = contract.getType();
            String methodId = type.toString();

            ///protocol.TransferContract
            //type.googleapis.com/protocol.TransferContract
            /**
             * 主币
             */
            if ("TransferContract".equals(methodId)) {

                final Contract.TransferContract transferAssetContract = contract.getParameter().unpack(Contract.TransferContract.class);
                final ByteString ownerAddress = transferAssetContract.getOwnerAddress();

                final String fromAddress = Base58Check.bytesToBase58(ownerAddress.toByteArray());
                final String toAddress = Base58Check.bytesToBase58(transferAssetContract.getToAddress().toByteArray());
                final BigDecimal amount = fromWei(new BigDecimal(transferAssetContract.getAmount()), 6);
                BlockTransaction.Transaction transactionVo = new BlockTransaction.Transaction();
                transactionVo.setUnit("TRX");
                transactionVo.setContractAddress("");
                transactionVo.setTransactionHash(hash);
                transactionVo.setFromAddress(fromAddress);
                transactionVo.setToAddress(toAddress);
                transactionVo.setMethodId(methodId);
                transactionVo.setAmount(amount);
                transactionVo.setAssetType(BlockTransaction.Transaction.AssetType.MAIN);
                transactionList.add(transactionVo);
                continue;
            }


            /**
             * trc10
             */
            //type.googleapis.com/protocol.TransferAssetContract
            if ("TransferAssetContract".equals(methodId)) {

                Contract.TransferAssetContract transferAssetContract = contract.getParameter().unpack(Contract.TransferAssetContract.class);
                final ByteString ownerAddress = transferAssetContract.getOwnerAddress();

                final String fromAddress = Base58Check.bytesToBase58(ownerAddress.toByteArray());
                final String toAddress = Base58Check.bytesToBase58(transferAssetContract.getToAddress().toByteArray());
                final BigDecimal amount = fromWei(new BigDecimal(transferAssetContract.getAmount()), 6);
                BlockTransaction.Transaction transactionVo = new BlockTransaction.Transaction();
                // transactionVo.setUnit("");
                transactionVo.setContractAddress(transferAssetContract.getAssetName().toStringUtf8());
                transactionVo.setTransactionHash(hash);
                transactionVo.setFromAddress(fromAddress);
                transactionVo.setToAddress(toAddress);
                transactionVo.setMethodId(methodId);
                transactionVo.setAmount(amount);
                transactionVo.setAssetType(BlockTransaction.Transaction.AssetType.TRC10);

                /**
                 * 查询货币单位和精度(可以放到业务层,判断是否为目标交易后再计算)
                 */
//                ContractToken contractToken = cacheService.findContractToken(transactionVo.getContractAddress(), BlockTransaction.Transaction.AssetType.TRC10);
//                if (contractToken.getStatus() == Status.FAILED) {
//                    continue;
//                }
//                transactionVo.setUnit(contractToken.getSymbol());

                transactionList.add(transactionVo);
                continue;
            }

            log.info("<=================其他协议:{}", contract.getParameter().getTypeUrl());
        }

        return transactionList;

    }

    @SneakyThrows
    public List<BlockTransaction.Transaction> getBlockTransactionByNumber(long height) {

        GrpcAPI.NumberMessage numberMessage = GrpcAPI.NumberMessage.newBuilder().setNum(height).build();

        /**
         * 包含区块log信息
         */
        final List<Response.TransactionInfo> transactionInfoList = getClient().blockingStub.getTransactionInfoByBlockNum(numberMessage).getTransactionInfoList();
        if (transactionInfoList.isEmpty()) {
            log.warn("<=======交易列表为空, blockNumber:{}", height);
            return null;
        }

        /**
         * 区块交易及hash fee
         */
        Response.BlockExtention blockExtention = getClient().blockingStub.getBlockByNum2(numberMessage);
        List<Response.TransactionExtention> transactionsList = blockExtention.getTransactionsList();

        if (transactionsList.isEmpty()) {
            log.info("<=======区块内交易列表为空, blockNumber:{}", height);
            return null;
        }

        if (transactionsList.size() != transactionInfoList.size()) {
            log.warn("<==============transactionInfoList长度不等于transactionsList===================");
            return null;
        }


        List<BlockTransaction.Transaction> transactionListOutPut = new ArrayList<>();
        for (int i = 0; i < transactionsList.size(); i++) {

            Response.TransactionInfo transactionInfo = transactionInfoList.get(i);
            String txHash = Hex.toHexString(transactionInfo.getId().toByteArray());
            //手续费
            BigDecimal fee = fromWei(BigDecimal.valueOf(transactionInfo.getFee()), 6);

            Response.ResourceReceipt receipt = transactionInfo.getReceipt();
            long netUsage = receipt.getNetUsage();
            BigDecimal netFee = fromWei(new BigDecimal(receipt.getNetFee()), 6);
            //long energyFee = receipt.getEnergyFee();//燃烧trx获取的能量
            long energyUsageTotal = receipt.getEnergyUsageTotal();//总共消耗的能量
            long energyUsage = receipt.getEnergyUsage();//质押消耗的能量
            BigDecimal energyFee = fromWei(BigDecimal.valueOf(receipt.getEnergyFee()), 6);//消耗掉的fee


            Response.TransactionExtention transactionExtention = transactionsList.get(i);
            Chain.Transaction transaction = transactionExtention.getTransaction();
            Chain.Transaction.raw rawData = transaction.getRawData();

            List<Chain.Transaction.Contract> contractList = rawData.getContractList();
            for (Chain.Transaction.Contract contract : contractList) {

                Chain.Transaction.Contract.ContractType type = contract.getType();
                String typeStr = type.toString();

                /**
                 * 主币
                 */
                if ("TransferContract".equals(typeStr)) {

                    final Contract.TransferContract transferAssetContract = contract.getParameter().unpack(Contract.TransferContract.class);
                    final ByteString ownerAddress = transferAssetContract.getOwnerAddress();

                    final String fromAddress = Base58Check.bytesToBase58(ownerAddress.toByteArray());
                    final String toAddress = Base58Check.bytesToBase58(transferAssetContract.getToAddress().toByteArray());
                    final BigDecimal amount = fromWei(new BigDecimal(transferAssetContract.getAmount()), 6);
                    BlockTransaction.Transaction transactionVo = new BlockTransaction.Transaction();
                    transactionVo.setUnit("TRX");
                    transactionVo.setContractAddress("");
                    transactionVo.setTransactionHash(txHash);
                    transactionVo.setFromAddress(fromAddress);
                    transactionVo.setToAddress(toAddress);
                    transactionVo.setMethodId(typeStr);
                    transactionVo.setAmount(amount);
                    transactionVo.setAssetType(BlockTransaction.Transaction.AssetType.MAIN);
                    transactionVo.setNetFee(netFee);
                    transactionVo.setNetUsage(netUsage);
                    transactionVo.setEnergyFee(energyFee);
                    transactionVo.setEnergyUsageTotal(energyUsageTotal);
                    transactionVo.setEnergyUsage(energyUsage);
                    transactionVo.setFee(fee);
                    transactionListOutPut.add(transactionVo);
                }

                /**
                 * trc10
                 */
                //type.googleapis.com/protocol.TransferAssetContract
                if ("TransferAssetContract".equals(typeStr)) {

                    Contract.TransferAssetContract transferAssetContract = contract.getParameter().unpack(Contract.TransferAssetContract.class);
                    final ByteString ownerAddress = transferAssetContract.getOwnerAddress();

                    final String fromAddress = Base58Check.bytesToBase58(ownerAddress.toByteArray());
                    final String toAddress = Base58Check.bytesToBase58(transferAssetContract.getToAddress().toByteArray());
                    final BigDecimal amount = fromWei(new BigDecimal(transferAssetContract.getAmount()), 6);
                    BlockTransaction.Transaction transactionVo = new BlockTransaction.Transaction();
                    // transactionVo.setUnit("");
                    transactionVo.setContractAddress(transferAssetContract.getAssetName().toStringUtf8());
                    transactionVo.setTransactionHash(txHash);
                    transactionVo.setFromAddress(fromAddress);
                    transactionVo.setToAddress(toAddress);
                    transactionVo.setMethodId(typeStr);
                    transactionVo.setAmount(amount);
                    transactionVo.setAssetType(BlockTransaction.Transaction.AssetType.TRC10);
                    transactionVo.setNetFee(netFee);
                    transactionVo.setNetUsage(netUsage);
                    transactionVo.setEnergyFee(energyFee);
                    transactionVo.setEnergyUsageTotal(energyUsageTotal);
                    transactionVo.setEnergyUsage(energyUsage);
                    transactionVo.setFee(fee);
                    transactionListOutPut.add(transactionVo);
                }


                String methodId = "";
                if ("TriggerSmartContract".equals(typeStr)) {
                    final Contract.TriggerSmartContract triggerSmartContract = contract.getParameter().unpack(Contract.TriggerSmartContract.class);
                    byte[] bytes = triggerSmartContract.getData().toByteArray();
                    byte[] methodByte = Arrays.copyOfRange(bytes, 0, 4);
                    methodId = "0x" + Hex.toHexString(methodByte);
                }

                /**
                 * erc20交易
                 */
                for (Response.TransactionInfo.Log transactionLog : transactionInfo.getLogList()) {

                    List<ByteString> topicsList = transactionLog.getTopicsList();
                    String transactionType = Hex.toHexString(topicsList.get(0).toByteArray());


                    switch (transactionType) {
                        case TopicEvent.TRANSFER_EVENT: {
                            ByteString address = transactionLog.getAddress();
                            final String contractAddress = Base58Check.bytesToBase58(byteMerger(new byte[]{65}, address.toByteArray()));

                            final String fromAddress = topicToAddress(topicsList.get(1).toByteArray());
                            final String toAddress = topicToAddress(topicsList.get(2).toByteArray());

                            BlockTransaction.Transaction transactionVo = new BlockTransaction.Transaction();
                            transactionVo.setContractAddress(contractAddress);
                            transactionVo.setTransactionHash(txHash);
                            transactionVo.setFromAddress(fromAddress);
                            transactionVo.setToAddress(toAddress);
                            transactionVo.setMethodId(methodId);
                            transactionVo.setAssetType(BlockTransaction.Transaction.AssetType.TRIGGER_SMART_CONTRACT);
                            transactionVo.setNetFee(netFee);
                            transactionVo.setNetUsage(netUsage);
                            transactionVo.setEnergyFee(energyFee);
                            transactionVo.setEnergyUsageTotal(energyUsageTotal);
                            transactionVo.setEnergyUsage(energyUsage);
                            transactionVo.setFee(fee);


                            byte[] data;
                            if (topicsList.size() > 3) {
                                data = topicsList.get(3).toByteArray();
                            } else {
                                data = transactionLog.getData().toByteArray();
                            }

                            if (data.length > 0) {
                                transactionVo.setValue(new BigInteger(1, data));
                                transactionVo.setAmount(new BigDecimal(transactionVo.getValue()));
                            } else {
                                transactionVo.setValue(BigInteger.ZERO);
                                transactionVo.setAmount(BigDecimal.ZERO);
                            }

                            transactionListOutPut.add(transactionVo);
                            break;
                        }
                        case TopicEvent.TRANSFER_SINGLE_EVENT:
                        case TopicEvent.TRANSFER_BATCH_EVENT: {
                            ByteString address = transactionLog.getAddress();
                            final String contractAddress = Base58Check.bytesToBase58(byteMerger(new byte[]{65}, address.toByteArray()));
                            final String fromAddress = topicToAddress(topicsList.get(2).toByteArray());
                            final String toAddress = topicToAddress(topicsList.get(3).toByteArray());
                            final ByteString data = transactionLog.getData();

                            BlockTransaction.Transaction transactionVo = new BlockTransaction.Transaction();
                            transactionVo.setContractAddress(contractAddress);
                            transactionVo.setTransactionHash(txHash);
                            transactionVo.setFromAddress(fromAddress);
                            transactionVo.setToAddress(toAddress);
                            transactionVo.setMethodId(methodId);
                            transactionVo.setAssetType(BlockTransaction.Transaction.AssetType.TRIGGER_SMART_CONTRACT);
                            transactionVo.setNetFee(netFee);
                            transactionVo.setNetUsage(netUsage);
                            transactionVo.setEnergyFee(energyFee);
                            transactionVo.setEnergyUsageTotal(energyUsageTotal);
                            transactionVo.setEnergyUsage(energyUsage);
                            transactionVo.setFee(fee);

                            final Map<BigInteger, BigInteger> nftMap;
                            if (transactionType.equals(TopicEvent.TRANSFER_SINGLE_EVENT)) {
                                nftMap = parseSingleTransfer1155Data(data);
                                //transactionVo.setNftMap(nftMap);
                            } else if (transactionType.equals(TopicEvent.TRANSFER_BATCH_EVENT)) {
                                nftMap = parseBatchTransfer1155Data(data);
                                //transactionVo.setNftMap(nftMap);
                            } else {
                                throw new RuntimeException("<===不可能出现的topic类型:" + transactionType);
                            }

                            if (nftMap == null) {
                                continue;
                            }

                            /**
                             * 如果是批量交易  则将批量交易转换成多个单笔交易
                             */
                            nftMap.forEach((tokenId, amount) -> {
                                BlockTransaction.Transaction transactionVo_ = new BlockTransaction.Transaction();
                                BeanUtils.copyProperties(transactionVo, transactionVo_);
                                transactionVo_.setValue(tokenId);
                                transactionVo_.setAmount(new BigDecimal(amount));
                                transactionListOutPut.add(transactionVo_);
                            });
                        }
                        default: {
                            log.debug("<=========未解析的类型transactionType:{}, txHash:{},height:{} ", transactionType, txHash, height);
                        }
                    }

                }
            }
        }

        // 对跨链内容进行普通填充
        transactionListOutPut.forEach(txnOutput -> {
            if (TRON_BRIDGE_FX.equalsIgnoreCase(txnOutput.getMethodId())) {
                // send to fx
                int targetChainId = ChainIdChecker.isMainNet(chainProperties.getChainId())
                        ? MainnetChainEnum.FX_EVM_MAINNET.id : TestnetChainEnum.FX_EVM_TESTNET.id;
                txnOutput.setCrossChainTxnCheck(true);
                txnOutput.setSourceChainId(chainProperties.getChainId());
                txnOutput.setSourceChainName(ChainServerHelper.findByChainId(chainProperties.getChainId()).getChainServerName());
                txnOutput.setTargetChainId(targetChainId);
                txnOutput.setTargetChainName(ChainServerHelper.findByChainId(targetChainId).getChainServerName());
            }

            if (FX_BRIDGE_TRON.equalsIgnoreCase(txnOutput.getMethodId())) {
                int sourceChainId = ChainIdChecker.isMainNet(chainProperties.getChainId())
                        ? MainnetChainEnum.FX_EVM_MAINNET.id : TestnetChainEnum.FX_EVM_TESTNET.id;
                // fx to tron
                txnOutput.setCrossChainTxnCheck(true);
                txnOutput.setSourceChainId(sourceChainId);
                txnOutput.setSourceChainName(ChainServerHelper.findByChainId(sourceChainId).getChainServerName());
                txnOutput.setTargetChainId(chainProperties.getChainId());
                txnOutput.setTargetChainName(ChainServerHelper.findByChainId(chainProperties.getChainId()).getChainServerName());
            }
        });

        return transactionListOutPut;
    }

    public static BigDecimal fromWei(BigDecimal number, int decimal) {
        return number.divide(BigDecimal.TEN.pow(decimal), 18, RoundingMode.DOWN);
    }


    public static String topicToAddress(byte[] bytes) {
        byte[] bytes1 = Arrays.copyOfRange(bytes, 11, bytes.length);
        bytes1[0] = 65;
        return Base58Check.bytesToBase58(bytes1);
    }


    public static byte[] byteMerger(byte[] bt1, byte[] bt2) {
        byte[] bt3 = new byte[bt1.length + bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }


    private static final String ERC721 = "0x80ac58cd";
    //private static final String ERC721Enumerable = "0x780e9d63";
    private static final String ERC721MetaData = "0x5b5e139f";
    private static final String[] ERC721s = new String[]{ERC721, ERC721MetaData};


    public boolean checkMethod(ByteString contractAddress) {
        // 一个奇怪的现象,有些合约调用任何不存在的方法都会返回1, 当遇到这种情况,不把他当做erc721
        Response.TransactionExtention checkMethod = triggerConstantContract(contractAddress, "this is check method", new ArrayList<>(), ImmutableList.of(TypeReference.create(Bool.class)));
        if (checkMethod.getConstantResultList().size() > 0) {
            final byte[] check = checkMethod.getConstantResult(0).toByteArray();
            if (check.length > 0) {
                byte[] bytes = ByteUtils.trimPrefixZero(ByteUtils.trimPrefixZero(check));
                if (bytes.length > 0) {
                    BigInteger bigInteger_ = new BigInteger(bytes);
                    if (BigInteger.ONE.compareTo(bigInteger_) == 0) {
                        return false;
                    }
                }

            }
        }
        return true;
    }


    public boolean isTRC1155(ByteString contractAddress) {

        Response.TransactionExtention transactionExtention = triggerConstantContract(contractAddress, "supportsInterface", Collections.singletonList(new Bytes4(Hex.decode("0xd9b67a26".substring(2)))), ImmutableList.of(TypeReference.create(Bool.class)));
        if (transactionExtention.getConstantResultList().size() > 0) {
            final byte[] constantResult = transactionExtention.getConstantResult(0).toByteArray();
            if (constantResult.length > 0) {
                byte[] bytes = ByteUtils.trimPrefixZero(constantResult);
                if (bytes.length > 0) {
                    BigInteger bigInteger = new BigInteger(bytes);
                    if (BigInteger.ONE.compareTo(bigInteger) == 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    public boolean isTRC721(ByteString contractAddress) {

        for (String ercInterface : ERC721s) {
            Response.TransactionExtention transactionExtention = triggerConstantContract(contractAddress, "supportsInterface", Collections.singletonList(new Bytes4(Hex.decode(ercInterface.substring(2)))), ImmutableList.of(TypeReference.create(Bool.class)));
            if (transactionExtention.getConstantResultList().size() > 0) {
                final byte[] constantResult = transactionExtention.getConstantResult(0).toByteArray();
                if (constantResult.length > 0) {
                    BigInteger bigInteger = new BigInteger(ByteUtils.trimPrefixZero(constantResult));
                    if (BigInteger.ONE.compareTo(bigInteger) == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String uri(ByteString contractAddress, Uint256 tokenId) {
        Response.TransactionExtention transactionExtention = triggerConstantContract(contractAddress, "uri", Collections.singletonList(tokenId), ImmutableList.of(TypeReference.create(Utf8String.class)));
        List<ByteString> constantResultList = transactionExtention.getConstantResultList();
        if (constantResultList.isEmpty()) {
            return null;
        }
        final byte[] constantResult = transactionExtention.getConstantResult(0).toByteArray();
        if (constantResult.length > 64) {
            byte[] bytes = Arrays.copyOfRange(constantResult, 64, constantResult.length);
            return new String(bytes).trim();
        }

        return "";
    }

    public String tokenUri(ByteString contractAddress, Uint256 tokenId) {
        Response.TransactionExtention transactionExtention = triggerConstantContract(contractAddress, "tokenURI", Collections.singletonList(tokenId), ImmutableList.of(TypeReference.create(Utf8String.class)));
        List<ByteString> constantResultList = transactionExtention.getConstantResultList();
        if (constantResultList.isEmpty()) {
            return null;
        }
        final byte[] constantResult = transactionExtention.getConstantResult(0).toByteArray();
        if (constantResult.length > 64) {
            byte[] bytes = Arrays.copyOfRange(constantResult, 64, constantResult.length);
            return new String(bytes).trim();
        }

        return "";
    }

    public Response.TransactionExtention triggerConstantContract(ByteString contractAddress, String name, List<Type> inputParameters, List<TypeReference<?>> outputParameters) {
        Function functionSymbol = new Function(name, inputParameters, outputParameters);
        String dataSymbol = FunctionEncoder.encode(functionSymbol);
        final ByteString bytesSymbol = ByteString.copyFrom(Hex.decode(dataSymbol));
        Contract.TriggerSmartContract requestSymbol = Contract.TriggerSmartContract.newBuilder()
                .setContractAddress(contractAddress)
                .setData(bytesSymbol).build();
        final Response.TransactionExtention transactionExtentionSymbol = this.getClient().blockingStub.triggerConstantContract(requestSymbol);
        return transactionExtentionSymbol;
    }


    public String trc20Name(ByteString contractAddress) {
        Response.TransactionExtention transactionExtention = this.triggerConstantContract(contractAddress, "name", new ArrayList<>(), ImmutableList.of(TypeReference.create(Utf8String.class)));
        List<ByteString> constantResultList = transactionExtention.getConstantResultList();
        if (!constantResultList.isEmpty()) {
            ByteString bytes = constantResultList.get(0);
            return new String(ByteUtils.trimPrefixZero(bytes.toByteArray())).trim();
        }
        return "";
    }

    /**
     * 填充合约信息(币种,精度,erc721)
     *
     * @param transactionList
     */
    public void fillContractInfo(List<BlockTransaction.Transaction> transactionList) {
        if (transactionList == null || transactionList.isEmpty()) {
            return;
        }
        for (BlockTransaction.Transaction transaction : transactionList) {

            if (transaction.getAssetType() != BlockTransaction.Transaction.AssetType.MAIN) {
                ContractToken contractToken = cacheService.findContractToken(transaction.getContractAddress(), transaction.getAssetType());

                if (contractToken.isNft()) {
                    String tokenUri = "";
                    if (contractToken.getEip() == 721) {
                        tokenUri = this.tokenUri(ByteString.copyFrom(Base58Check.base58ToBytes(transaction.getContractAddress())), new Uint256(transaction.getValue()));
                    } else if (contractToken.getEip() == 1155) {
                        tokenUri = this.uri(ByteString.copyFrom(Base58Check.base58ToBytes(transaction.getContractAddress())), new Uint256(transaction.getValue()));
                        if (tokenUri.contains("{") && tokenUri.contains("}")) {
                            tokenUri = tokenUri.replaceAll("\\\\", "");
                            final String str1 = tokenUri.substring(0, tokenUri.lastIndexOf("{"));
                            final String str2 = tokenUri.substring(tokenUri.lastIndexOf("}") + 1);
                            tokenUri = str1 + transaction.getValue() + str2;
                        }
                    } else {
                        throw new RuntimeException("不可能出现的类型");
                    }

                    NftUtils.NftInfo nftInfo = NftUtils.parseNft(tokenUri);
                    transaction.setName(contractToken.getName());
                    if (!com.google.common.base.Strings.isNullOrEmpty(contractToken.getName())) {
                        transaction.setName(nftInfo.getName());
                    } else if (!com.google.common.base.Strings.isNullOrEmpty(nftInfo.getName())) {
                        transaction.setName("NFT-1155");
                    }

                    transaction.setName(contractToken.getName());
                    transaction.setNftInfo(nftInfo);
                    transaction.setTokenUri(tokenUri);
                    transaction.setEip(String.valueOf(contractToken.getEip()));
                } else {
                    transaction.setAmount(ByteUtils.fromWei(new BigDecimal(transaction.getValue()), contractToken.getDecimal().intValue()));
                    transaction.setUnit(contractToken.getSymbol());
                    transaction.setEip("20");
                }
            }
        }
    }


    public ContractToken findContractToken(String contractAddress, BlockTransaction.Transaction.AssetType assetType) {

        /**
         * 去链上查询
         */
        ContractToken contractToken = new ContractToken();
        contractToken.setSymbol("");
        contractToken.setDecimal(BigInteger.valueOf(6));
        contractToken.setStatus(Status.SUCCESS);

        StringBuilder sb = new StringBuilder();


        /**
         * trc10
         */
        if (assetType == BlockTransaction.Transaction.AssetType.TRC10) {
            String symbol = this.getTrc10Symbol(contractAddress);
            log.warn("<=====1==========symbole:{}", symbol);
            if (symbol == null) {
                contractToken.setStatus(Status.FAILED);
                sb.append("symbol not found");
            } else {
                contractToken.setSymbol(symbol);
            }
        }


        if (assetType == BlockTransaction.Transaction.AssetType.TRIGGER_SMART_CONTRACT) {
            String symbol = this.getTrc20Symbol(contractAddress);
            log.warn("<=====2==========symbole:{}", symbol);
            if (Strings.isEmpty(symbol)) {
                contractToken.setStatus(Status.FAILED);
                sb.append("symbol not found ");
            } else {
                contractToken.setSymbol(symbol);
            }


            BigInteger decimal = this.getDecimal(contractAddress);
            if (decimal == null) {
                contractToken.setStatus(Status.FAILED);
                sb.append("decimal not found ");
            } else {
                contractToken.setDecimal(decimal);
            }


            /**
             * 判断是否为721
             */
            if (decimal == null) {
                ByteString byteString = ByteString.copyFrom(Base58Check.base58ToBytes(contractAddress));

                if (checkMethod(byteString)) {
                    if (isTRC1155(byteString)) {
                        contractToken.setNft(true);
                        contractToken.setEip(1155);
                        String name = trc20Name(byteString);
                        contractToken.setName(name);
                    } else if (isTRC721(byteString)) {
                        contractToken.setNft(true);
                        contractToken.setEip(721);
                        String name = trc20Name(byteString);
                        contractToken.setName(name);
                    }
                }
            }
        }

        return contractToken;

    }


    public static Map<BigInteger, BigInteger> parseSingleTransfer1155Data(ByteString data) {
        BigInteger tokenId = new BigInteger(1, data.substring(0, 32).toByteArray());
        BigInteger amount = new BigInteger(1, data.substring(33).toByteArray());
        final HashMap<BigInteger, BigInteger> map = Maps.newHashMap();
        map.put(tokenId, amount);
        return map;
    }


    public static Map<BigInteger, BigInteger> parseBatchTransfer1155Data(ByteString data) {

        if (data.size() < 96) {
            return null;
        }

        /**
         * nft 的个数
         */
        int num = new BigInteger(1, data.substring(65, 96).toByteArray()).intValue();
        int index = 96;
        List<BigInteger> tokenIds = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            final ByteString substring = data.substring(++index, index = index + 31);
            tokenIds.add(new BigInteger(substring.toByteArray()));
        }


        List<BigInteger> amounts = new ArrayList<>();
        index = index + 32;
        for (int i = 0; i < num; i++) {
            final ByteString substring = data.substring(++index, index = index + 31);
            amounts.add(new BigInteger(substring.toByteArray()));
        }

        Map<BigInteger, BigInteger> map = new HashMap<>();
        for (int i = 0; i < tokenIds.size(); i++) {
            map.put(tokenIds.get(i), amounts.get(i));
        }
        return map;
    }


}
