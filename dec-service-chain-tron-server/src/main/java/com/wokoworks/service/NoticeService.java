package com.wokoworks.service;

import com.wokoworks.chain.vo.BlockTransaction;

/**
 * @Author: 飞
 * @Date: 2021/10/13 10:54
 */
public interface NoticeService {

    /**
     * 区块到账通知
     * @param blockTransaction
     */
    boolean notice(BlockTransaction blockTransaction);
}
