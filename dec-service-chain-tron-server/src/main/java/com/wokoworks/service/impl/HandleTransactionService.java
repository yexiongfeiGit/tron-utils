package com.wokoworks.service.impl;

import com.wokoworks.chain.RpcClient;
import com.wokoworks.chain.notice.client.WokoNoticeClient;
import com.wokoworks.chain.vo.BlockTransaction;
import com.wokoworks.chain.vo.NewBlockHead;
import com.wokoworks.config.ChainProperties;
import com.wokoworks.repository.BlockInfoRepository;
import com.wokoworks.repository.CurrencyRepository;
import com.wokoworks.repository.SubscribeAddressRepository;
import com.wokoworks.repository.SubscribeApplicationAddressRepository;
import com.wokoworks.service.NoticeService;
import com.wokoworks.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
public class HandleTransactionService {

    @Autowired
    private  BlockInfoRepository blockInfoRepository;
    @Autowired
    private  DispatchTransactionService dispatchTransactionService;
    @Autowired
    private  SubscribeAddressRepository subscribeAddressRepository;
    @Autowired
    private  SubscribeApplicationAddressRepository subscribeApplicationAddressRepository;
    @Autowired
    private  CurrencyRepository currencyRepository;
    @Autowired
    private  WokoNoticeClient wokoNoticeClient;
    @Autowired
    private ChainProperties chainProperties;
    @Autowired
    private RpcClient rpcClient;
    @Autowired
    private NoticeService noticeService;
//    @Autowired
//    private MsgProducer msgProducer;


    public boolean handleBlockTransaction(NewBlockHead newBlockHead) {

        log.debug("==> 处理交易blockNumber: {}, blockHash:{}", newBlockHead.getBlockNumber(), newBlockHead.getBlockHash());
        final long blockNumber = newBlockHead.getBlockNumber();
        final boolean result = notice(newBlockHead);
        final long now = System.currentTimeMillis();
        if (result) {
            blockInfoRepository.updateStatusUpdateDtByBlockNumber(blockNumber, BlockInfo.Status.COMPLETE, now);
        } else {
            List<Short> whereStatuss = new ArrayList<>();
            whereStatuss.add(BlockInfo.Status.PENDING.value);
            whereStatuss.add(BlockInfo.Status.PROCESSING.value);
            blockInfoRepository.updateStatusFailPushCountUpdateDtByBlockNumberAndStatuss(blockNumber, whereStatuss, BlockInfo.Status.FAIL, 1, now);
            dispatchTransactionService.notifyConsume();
        }
        log.debug("<== 处理交易结束 result:{}, blockNumber: {}, blockHash:{}", result, newBlockHead.getBlockNumber(), newBlockHead.getBlockHash());
        return result;
    }

    private boolean notice(NewBlockHead newBlockHead) {
        try {
            List<BlockTransaction.Transaction> transactionList = rpcClient.getBlockTransactionByNumber(newBlockHead.getBlockNumber());
            if (transactionList == null || transactionList.isEmpty()) {
                return true;
            }

            BlockTransaction blockTransaction = new BlockTransaction();
            blockTransaction.setTransactionList(transactionList);
            blockTransaction.setBlockDt(newBlockHead.getBlockDt());
            blockTransaction.setBlockHash(newBlockHead.getBlockHash());
            blockTransaction.setBlockNumber(newBlockHead.getBlockNumber());
            noticeService.notice(blockTransaction);

        }catch (Exception e) {
            log.warn(e.getMessage(), e);
            return false;
        }

        return true;

    }
}
