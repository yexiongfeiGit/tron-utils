package com.wokoworks.mq;

import com.alibaba.fastjson.JSON;
import com.wokoworks.chain.vo.BlockTransaction;
import com.wokoworks.config.ChainProperties;
import com.wokoworks.mq.vo.MsgBlockInfo;
import com.wokoworks.utils.NftUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Author: 飞
 * @Date: 2021/11/12 11:34
 */
@Slf4j
@Component
public class MsgProducer {

    private static final String EXCHANGE_NAME = "chain.scan.exchange";
    private static final ExecutorService executorService = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(100), new ThreadPoolExecutor.CallerRunsPolicy());

    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private ChainProperties chainProperties;


    /**
     * 向mq发送消息
     * @param msg
     */
    public void sendToExchange(String msg) {
        amqpTemplate.convertAndSend(EXCHANGE_NAME,"",  msg);
    }


    public void sendAssetService(BlockTransaction blockTransaction) {

        List<BlockTransaction.Transaction> transactionList = blockTransaction.getTransactionList();

        if (transactionList == null || transactionList.isEmpty()) {
            return;
        }

        executorService.execute(() -> {

            List<MsgBlockInfo.Transaction> msgTransactionList = transactionList.stream().map((transaction) -> {
                MsgBlockInfo.Transaction msgTransaction = new MsgBlockInfo.Transaction();
                msgTransaction.setTransactionHash(transaction.getTransactionHash());
                msgTransaction.setFromAddress(transaction.getFromAddress());
                msgTransaction.setToAddress(transaction.getToAddress());
                msgTransaction.setSymbol(transaction.getUnit());
                msgTransaction.setContractAddress(transaction.getContractAddress());
                msgTransaction.setDecimals(transaction.getDecimals());
                msgTransaction.setEip(transaction.getEip());
                msgTransaction.setAmount(transaction.getAmount());
                msgTransaction.setFee(transaction.getFee());
                if ("721".equals(transaction.getEip()) || "1155".equals(transaction.getEip())) {
                    msgTransaction.setName(transaction.getName());
                    msgTransaction.setTokenId(transaction.getValue().intValue());
                    msgTransaction.setNft(true);
                    NftUtils.NftInfo nftInfo = transaction.getNftInfo();
                    if (nftInfo != null) {
                        msgTransaction.setFile(nftInfo.getInnerFile());
                        msgTransaction.setFileType(nftInfo.getInnerFileType().value);
                    } else {
                        msgTransaction.setFile("");
                        msgTransaction.setFileType(0);
                    }
                }
                return msgTransaction;
            }).collect(Collectors.toList());
            MsgBlockInfo msgBlockInfo = new MsgBlockInfo();
            msgBlockInfo.setChainCode(chainProperties.getChainCode());
            msgBlockInfo.setBlockNum(blockTransaction.getBlockNumber());
            msgBlockInfo.setBlockHash(blockTransaction.getBlockHash());
            msgBlockInfo.setBlockDt(blockTransaction.getBlockDt());
            msgBlockInfo.setTransactionList(msgTransactionList);

            try {
                sendToExchange(JSON.toJSONString(msgBlockInfo));
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }


        });
    }



}
