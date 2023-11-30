// auto generator by wokoworks 2020年10月22日 下午5:46:52
package com.wokoworks.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * table name: currency
 *
 * @date 2020年10月22日 下午5:46:52
 * @author luobing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Currency {
    private int id;

    /**
     * 币种名称
     */
    private String unit;

    /**
     * 合约地址
     */
    private String contractAddress;

    /**
     * 币种合约精度
     */
    private int decimals;

    /**
     * 创建时间
     */
    private long dt;

    /**
     * 状态
     */
    private int status;

    private String remark;

//    public enum Status {
//        FAILED, SUCCESS
//    }
}
