// auto generator by wokoworks 2020年7月29日 下午6:46:26
package com.wokoworks.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * table name: block_info
 *
 * @date 2020年7月29日 下午6:46:26
 * @author luobing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockInfo {
    private int id;

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
     * 节点名
     */
    private String nodeName;

    /**
     * 状态 0:待处理; 1:处理中; 2:处理失败; 3:处理完成; 4:异常数据
     */
    private short status;

    /**
     * 抢占处理权次数
     */
    private int seizeCount;

    private int failPushCount;
    private long forkDt;
    /**
     * 修改时间
     */
    private long updateDt;

    /**
     * 出块时间
     */
    private long blockDt;

    /**
     * 创建时间
     */
    private long dt;

    public enum Status {
        PENDING(0, "待处理"),
        PROCESSING(1, "处理中"),
        FAIL(2, "处理失败"),
        COMPLETE(3, "处理完成"),
        ERROR(4, "异常数据"),
        ;

        public final short value;
        public final String remark;

        Status(int value, String remark) {
            this.value = (short) value;
            this.remark = remark;
        }

        public static Status valueOf(int value) {
            for (Status status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            return null;
        }
    }
}
