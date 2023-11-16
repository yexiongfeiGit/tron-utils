// auto generator by wokoworks 2020年10月22日 下午5:46:52
package com.wokoworks.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * table name: subscribe_application_address
 *
 * @date 2020年10月22日 下午5:46:52
 * @author luobing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeApplicationAddress {
    private int id;

    /**
     * 订阅项目编号
     */
    private String appNum;

    /**
     * 订阅地址id
     */
    private int subscribeAddressId;

    /**
     * 创建时间
     */
    private long dt;
}
