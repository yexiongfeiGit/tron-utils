package com.wokoworks.service.impl;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.wokoworks.chain.RpcClient;
import com.wokoworks.chain.vo.NewBlockHead;
import com.wokoworks.repository.BlockInfoRepository;
import com.wokoworks.vo.BlockInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Service
public class HandleNewHeadService {
    private static volatile long lastBlockNumber;
    @Autowired
    private  BlockInfoRepository blockInfoRepository;
    @Autowired
    private  DispatchTransactionService dispatchTransactionService;
    private  String nodeName = "tron" ;
    @Autowired
    private  TransactionTemplate transactionTemplate;
    @Autowired
    private RpcClient rpcClient;



    /**
     * 处理block
     * @param
     */
    public void handleBlock(NewBlockHead newBlockHead) {
        log.debug("==> 开始处理块 newBlockHead: {}, nodeName:{}", newBlockHead, nodeName);
        //记录一下节点最新块(只是记录方便找问题，代码逻辑上没用到)
        //addNodeLastBlock(newBlockHead);

        final long blockNumber = newBlockHead.getBlockNumber();
        if (blockNumber <= lastBlockNumber) {
            log.debug("<== 已处理 blockNumber:{}, lastBlockNumber:{}", blockNumber, lastBlockNumber);
            return;
        }
        lastBlockNumber = newBlockHead.getBlockNumber();

        //当前节点块高度
        Optional<BlockInfo> blockInfoOptional = blockInfoRepository.findByBlockNumber(blockNumber);
        //如果区块高度已被处理则直接结束
        if (blockInfoOptional.isPresent()) {
            log.debug("<== nodeName:{}, blockNumber:{} 已被处理", nodeName, newBlockHead.getBlockNumber());
            return;
        }


        Optional<BlockInfo> blockInfoLastOptional = blockInfoRepository.findLast();
        if (blockInfoLastOptional.isPresent() && newBlockHead.getBlockNumber() <= blockInfoLastOptional.get().getBlockNumber()) {
            //节点上块小于数据库块，则不用处理
            log.debug("<== nodeName:{}, blockNumber:{} 数据库块大于节点块", nodeName, newBlockHead.getBlockNumber());
            return;
        }

        boolean result = handleBlockInfo(newBlockHead, blockInfoLastOptional.orElse(null));
        log.debug("==> 处理块结束result:{}, blockHeader: {}, nodeName:{}", result, newBlockHead, nodeName);
    }


    /**
     * 处理块
     * @param newBlockHead
     * @return
     */
    private boolean handleBlockInfo(NewBlockHead newBlockHead, BlockInfo blockInfoLast) {
        //当前节点块高度
        long blockNumber = newBlockHead.getBlockNumber();
        Optional<BlockInfo> blockInfoOptional = blockInfoRepository.findByBlockNumber(blockNumber - 1);

        //判断节点块高度和数据库块高度是否一致
        if (blockInfoOptional.isPresent()) {
            BlockInfo lastBlockInfo = blockInfoOptional.get();
            //正常出快没分叉
            if (newBlockHead.getParentHash().equals(lastBlockInfo.getBlockHash())) {
                return addBlockInfoHandleTransaction(newBlockHead);
            } else {
                //分叉情况
                log.warn("<==========块出现分叉");
                return handleForkBlock(newBlockHead, lastBlockInfo);
            }
        } else {
            //节点高度比数据库高度高于2个以上则补上
            if (blockInfoLast == null) {
                //如果数据库还没块
                return addBlockInfoHandleTransaction(newBlockHead);
            }
            //节点高度比数据库高度高于2个以上则补上
            return handleLackBlock(newBlockHead);
        }
    }

    /**
     * 节点高度比数据库高度高于2个以上补快
     * @param newBlockHead
     */
    private boolean handleLackBlock(NewBlockHead newBlockHead) {

        Optional<BlockInfo> blockInfoLastOptional = blockInfoRepository.findLast();
        if (!blockInfoLastOptional.isPresent()) {
            return false;
        }
        BlockInfo blockInfoLast = blockInfoLastOptional.get();

        long blockNumber = newBlockHead.getBlockNumber();
        //数据库last blockNumber < x <= 节点最新块 blockNumber
        long startBlockNumber = blockInfoLast.getBlockNumber() + 1;
        log.debug("节点高度比数据库高度高于2个以上补快startBlockNumber:{}, blockNumber:{}", startBlockNumber, blockNumber);

        for (long i = startBlockNumber; i < blockNumber; i++) {
            NewBlockHead blockHeader = rpcClient.getBlockHeaderByBlockNumber(i);
            boolean result = handleBlockInfo(blockHeader, blockInfoLast);
            if (!result) {
                return false;
            }
        }
        return addBlockInfoHandleTransaction(newBlockHead);
    }

    /**
     * 块分叉处理
     * @param newHead
     * @return
     */
    private boolean handleForkBlock(NewBlockHead newHead, BlockInfo blockInfo) {
        //找出要更新block
        List<ForkBlock> forkingBlockList = lookUpFindBlock(newHead);
        log.info("<==处理分叉区块:{}", JSON.toJSONString(forkingBlockList, true));

        //分叉同步块(数据库数据和链上数据同步)
        Boolean result = transactionTemplate.execute(status -> {

            long now = System.currentTimeMillis();

            /**
             * 更新旧的区块
             */
            for (int i = forkingBlockList.size() - 1; i >= 0; i--) {
                NewBlockHead item = forkingBlockList.get(i).getNewBlockHead();
                final BlockInfo blockInfoItem = forkingBlockList.get(i).getBlockInfo();
                log.debug("分叉数据 newBlockHead:{}, nodeName:{}", item, nodeName);

                int effectRow = blockInfoRepository.updateBlockHashAndParentHashAndNodeNameAndStatusAndForkDtByIdAndOldBlockHash(blockInfoItem.getId(), blockInfoItem.getBlockHash(),
                        item.getBlockHash(), item.getParentHash(), nodeName, BlockInfo.Status.PENDING, now, item.getBlockDt());
                if (effectRow != 1){
                    status.setRollbackOnly();
                    log.warn("<== updateBlockHashAndParentHashAndStatusAndUpdateDtByBlockNumber更新失败 blockNumber:{}, nodeName:{}", item.getBlockNumber(), nodeName);
                    return false;
                }
            }

            /**
             * 保存当前新的区块
             */
            boolean flag = addBlockInfo(newHead);
            if (!flag) {
                status.setRollbackOnly();
                return false;
            }

            return true;
        });

        //如果保存或更新blockInfo失败则结束
        if (result == null || !result) {
            return false;
        }

        dispatchTransactionService.notifyConsume();
        return true;
    }

    /**
     * 向上查找数据库块和节点块到一致
     * @param
     * @return
     */
    private List<ForkBlock> lookUpFindBlock(NewBlockHead newHead) {

        String parentHash = newHead.getParentHash();

        List<ForkBlock> list = new ArrayList<>();
        for (long  number = newHead.getBlockNumber() - 2; number < newHead.getBlockNumber() - 20; number--) {
            NewBlockHead updateBlockHead = rpcClient.getBlockHeaderByBlockNumber(number);
            Optional<BlockInfo> blockInfoOptional = blockInfoRepository.findByBlockNumber(number);
            if (!blockInfoOptional.isPresent()) {
                break;
            }
            if (updateBlockHead.getBlockHash().equals(parentHash)) {
                break;
            }
            parentHash = updateBlockHead.getParentHash();

            ForkBlock forkBlock = new ForkBlock();
            forkBlock.setNewBlockHead(updateBlockHead);
            forkBlock.setBlockInfo(blockInfoOptional.get());
            list.add(forkBlock);
        }

        return list;
    }

    /**
     * 添加BlockInfo并处理交易
     * @param newBlockHead
     * @return
     */
    private boolean addBlockInfoHandleTransaction(NewBlockHead newBlockHead) {
        boolean result = addBlockInfo(newBlockHead);
        if (!result) {
            return false;
        }
        dispatchTransactionService.notifyConsume();
        return true;
    }

    private boolean addBlockInfo(NewBlockHead newBlockHead) {
        BlockInfo blockInfo = new BlockInfo();
        blockInfo.setBlockHash(newBlockHead.getBlockHash());
        blockInfo.setBlockNumber(newBlockHead.getBlockNumber());
        blockInfo.setParentHash(newBlockHead.getParentHash());
        blockInfo.setNodeName(nodeName);
        blockInfo.setStatus(BlockInfo.Status.PENDING.value);
        blockInfo.setDt(System.currentTimeMillis());
        blockInfo.setForkDt(0);
        blockInfo.setUpdateDt(blockInfo.getDt());
        blockInfo.setBlockDt(newBlockHead.getBlockDt());

        Optional<BlockInfo> blockInfoOptional = blockInfoRepository.findByBlockNumber(newBlockHead.getBlockNumber());
        if (blockInfoOptional.isPresent()) {
            return false;
        }
        int result = blockInfoRepository.saveOrUpdate(blockInfo);

        log.debug("保存blockInfo blockNumber:{}", blockInfo.getBlockNumber());

        if (result <= 0) {
            log.info("<== blockInfo保存失败 blockInfo:{}", blockInfo);
            return false;
        }
        //并发情况只有最先入库的抢占处理权成功
        if (result >= 2) {
            log.debug("<== 抢占处理权失败 result:{}, blockInfo:{}", result, blockInfo);
            return false;
        }
        return true;
    }


    @Data
    private static class ForkBlock {
        private NewBlockHead newBlockHead;
        private BlockInfo blockInfo;
    }
}
