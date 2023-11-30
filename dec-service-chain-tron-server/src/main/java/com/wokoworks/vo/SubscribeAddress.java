// auto generator by wokoworks 2020年10月22日 下午5:46:52
package com.wokoworks.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * table name: subscribe_address
 *
 * @date 2020年10月22日 下午5:46:52
 * @author luobing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeAddress {
    private int id;

    /**
     * 订阅地址
     */
    private String address;

    /**
     * 创建时间
     */
    private long dt;
}
