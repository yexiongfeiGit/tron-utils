package com.wokoworks.chain;

import com.purgeteam.dynamic.config.starter.event.ActionConfigEvent;
import com.wokoworks.chain.vo.NewBlockHead;
import com.wokoworks.service.impl.DispatchTransactionService;
import com.wokoworks.service.impl.HandleNewHeadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * @Author: 飞
 * @Date: 2021/10/8 14:04
 */
@Slf4j
@Component
public class ChainScaner implements ApplicationListener {

    @Autowired
    private RpcClient rpcClient;

    @Autowired
    private HandleNewHeadService handleNewHeadService;
    @Autowired
    private DispatchTransactionService dispatchTransactionService;


    private boolean isRunning = false;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        /**
         * spring启动
         */
        if (event instanceof ApplicationReadyEvent) {
            rpcClient.init();
            isRunning = true;
            dispatchTransactionService.run();
        }

        /**
         * 更新nacos配置
         */
        if (event instanceof ActionConfigEvent) {
            lastBlockNum = 0;
            rpcClient.init();
        }

    }


    private static volatile long lastBlockNum = 0;

//    // TODO 自测入口
//    @Scheduled(cron = "0/3 * * * * ?")
//    public void chainScaner() {
//        while (isRunning) {
//            NewBlockHead newBlockHead = rpcClient.getBlockHeader();
//            if (newBlockHead.getBlockNumber() > lastBlockNum) {
//                lastBlockNum = newBlockHead.getBlockNumber();
//                handleNewHeadService.handleBlock(newBlockHead);
//                continue;
//            }
//            break;
//        }
//    }


}
