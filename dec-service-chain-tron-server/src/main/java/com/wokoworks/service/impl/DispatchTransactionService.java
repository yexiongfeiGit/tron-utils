package com.wokoworks.service.impl;

import com.wokoworks.chain.vo.NewBlockHead;
import com.wokoworks.config.ChainProperties;
import com.wokoworks.repository.BlockInfoRepository;
import com.wokoworks.utils.AbstractListTaskHandler;
import com.wokoworks.vo.BlockInfo;
import io.functionx.consumetask.ConsumeTask;
import io.functionx.consumetask.ThreadPoolBatchConsumeTaskDispatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DispatchTransactionService {

    @Autowired
    private BlockInfoRepository blockInfoRepository;
    @Autowired
    private BeanFactory beanFactory;
    private ConsumeTask consumeTaskDesc;
    @Autowired
    private ChainProperties chainProperties;

    public List<BlockHeadTaskData> searchWaitProcess(SearchType searchType, int serviceCount, int serviceIndex, int pageSize) {
        while (true) {
            List<Short> whereStatuss = new ArrayList<>();
            whereStatuss.add(BlockInfo.Status.PENDING.value);
            whereStatuss.add(BlockInfo.Status.FAIL.value);

            List<BlockInfo> blockInfoList;
            switch (searchType) {
                case Desc:
                    blockInfoList = blockInfoRepository.findByInStatussOrderDesc(whereStatuss, serviceCount, serviceIndex, pageSize, chainProperties.getRetryCount());
                    break;
                case Asc:
                    blockInfoList = blockInfoRepository.findByInStatussOrderAsc(whereStatuss, serviceCount, serviceIndex, pageSize, chainProperties.getRetryCount());
                    break;
                default:
                    return new ArrayList<>(0);
            }


            if (blockInfoList.isEmpty()) {
                //分配给自己块处理完后抢其他块
                blockInfoList = blockInfoRepository.findByInStatussOrderAsc(whereStatuss, pageSize, chainProperties.getRetryCount());
            }
            if (blockInfoList.isEmpty()) {
                log.debug("<== 待重试块数据为空searchType:{}", searchType);
                return new ArrayList<>(0);
            }
            log.info("重试块数据searchType:{} count: {}, 当前服务serviceIndex:{}, 总服务数serviceCount:{}", searchType, blockInfoList.size(), serviceIndex, serviceCount);

            List<BlockHeadTaskData> blockHeadTaskDataList = new ArrayList<>();
            final long now = System.currentTimeMillis();
            for (BlockInfo blockInfo : blockInfoList) {

                int effectRow = blockInfoRepository.updateStatusUpdateDtByIdAndStatuss(blockInfo.getId(), whereStatuss, BlockInfo.Status.PROCESSING, now);
                if (effectRow != 1) {
                    continue;
                }

                NewBlockHead newBlockHead = new NewBlockHead();
                newBlockHead.setBlockHash(blockInfo.getBlockHash());
                newBlockHead.setBlockNumber(blockInfo.getBlockNumber());
                newBlockHead.setParentHash(blockInfo.getParentHash());
                newBlockHead.setBlockDt(blockInfo.getBlockDt());

                BlockHeadTaskData blockHeadTaskData = new BlockHeadTaskData();
                blockHeadTaskData.setNewBlockHead(newBlockHead);
                blockHeadTaskData.setNodeName(blockInfo.getNodeName());

                blockHeadTaskDataList.add(blockHeadTaskData);
            }
            if (blockHeadTaskDataList.isEmpty()) {
                //抢不到继续抢
                continue;
            }

            return blockHeadTaskDataList;
        }
    }

    public void notifyConsume() {
        consumeTaskDesc.notifyConsume();
    }


    public void run() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        log.info("availableProcessors:{}", availableProcessors);
        availableProcessors = Math.max(availableProcessors, 2);

        //从最新的开始处理
        consumeTaskDesc = new ThreadPoolBatchConsumeTaskDispatch<>(availableProcessors, availableProcessors*2, 3, TimeUnit.MINUTES, new ArrayBlockingQueue<>(3), "desc",
                new AbstractListTaskHandler<BlockHeadTaskData>(beanFactory) {
                    @Override
                    public List<BlockHeadTaskData> searchTask(int serviceCount, int serviceIndex) {
                        return searchWaitProcess(SearchType.Desc, serviceCount, serviceIndex, 2);
                    }

                    @Override
                    public void handleTask(BlockHeadTaskData blockHeadTaskData) {
                        final NewBlockHead newBlockHead = blockHeadTaskData.getNewBlockHead();
                        long startTime = System.currentTimeMillis();

                        HandleTransactionService handleTransactionService = beanFactory.getBean(HandleTransactionService.class);//new HandleTransactionService(beanFactory, new BalancerBlockProvider(blockHeadTaskData.getNodeName()));
                        final boolean result = handleTransactionService.handleBlockTransaction(newBlockHead);

                        long endTime = System.currentTimeMillis();
                        log.debug("从新块开始处理 耗时:{}, blockNumber:{}, result:{}, threadName:{}", endTime-startTime, newBlockHead.getBlockNumber(), result, Thread.currentThread().getName());
                    }
                });
        consumeTaskDesc.startConsume();

        //从最久的开始处理
        ConsumeTask consumeTaskAsc = new ThreadPoolBatchConsumeTaskDispatch<>(1, availableProcessors, 3, TimeUnit.MINUTES, new ArrayBlockingQueue<>(2), "asc",
                new AbstractListTaskHandler<BlockHeadTaskData>(beanFactory) {
                    @Override
                    public List<BlockHeadTaskData> searchTask(int serviceCount, int serviceIndex) {
                        return searchWaitProcess(SearchType.Asc, serviceCount, serviceIndex, 2);
                    }

                    @Override
                    public void handleTask(BlockHeadTaskData blockHeadTaskData) {
                        final NewBlockHead newBlockHead = blockHeadTaskData.getNewBlockHead();
                        long startTime = System.currentTimeMillis();

                        //HandleTransactionService handleTransactionService = new HandleTransactionService(beanFactory, new BalancerBlockProvider(blockHeadTaskData.getNodeName()));
                        HandleTransactionService handleTransactionService = beanFactory.getBean(HandleTransactionService.class);
                        final boolean result = handleTransactionService.handleBlockTransaction(newBlockHead);

                        long endTime = System.currentTimeMillis();
                        log.debug("从历史块开始处理 耗时:{}, blockNumber:{}, result:{}, threadName:{}", endTime - startTime, newBlockHead.getBlockNumber(), result, Thread.currentThread().getName());
                    }
                });
        consumeTaskAsc.startConsume();
    }






    enum SearchType {
        Desc,
        Asc;
    }
    @Data
    private static class BlockHeadTaskData {
        private NewBlockHead newBlockHead;
        private String nodeName;
    }
}
