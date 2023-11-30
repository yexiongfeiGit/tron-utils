package com.wokoworks.chain.tron.common.codes.params;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;

public class SubscribeParam {

    @Data
    public static class SubscribeAddressInput {
        @NotBlank
        @ApiModelProperty(value = "项目授权码")
        private String appNum;
        @NotNull
        @ApiModelProperty(value = "订阅地址")
        private Set<String> addressSet;
    }

    @Data
    public static class UnSubscribeAddressInput {
        @NotBlank
        @ApiModelProperty(value = "项目授权码")
        private String appNum;
        @NotNull
        @ApiModelProperty(value = "订阅地址")
        private Set<String> addressSet;
    }
}
