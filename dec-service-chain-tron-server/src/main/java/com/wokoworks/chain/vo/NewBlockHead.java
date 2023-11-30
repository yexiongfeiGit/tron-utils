package com.wokoworks.chain.vo;

import lombok.Data;

/**
 * @Author: 飞
 * @Date: 2021/10/8 14:08
 */
@Data
public class NewBlockHead {
    /**
     * 区块Hash
     */
    private String blockHash;

    /**
     * 块高度
     */
    private long blockNumber;

    /**
     * 上一个块Hash
     */
    private String parentHash;

    /**
     * 出块时间
     */
    private long blockDt;
}
