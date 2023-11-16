package com.wokoworks.chain.tron.client;

import com.wokoworks.chain.tron.common.codes.codes.SubscribeCode;
import com.wokoworks.chain.tron.common.codes.params.SubscribeParam;
import com.wokoworks.framework.commons.data.CallValue;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Component
@FeignClient(name = "dec-service-chain-ethereum")
@RequestMapping("/subscribe")
public interface WokoSubscribeEthereumClient {

    @PostMapping("/subscribeAddress")
    @ApiOperation("订阅地址")
    CallValue<Void, SubscribeCode.SubscribeAddressCode> subscribeAddress(@RequestBody SubscribeParam.SubscribeAddressInput input);

    @PostMapping("/unSubscribeAddress")
    @ApiOperation("取消订阅地址")
    CallValue<Void, SubscribeCode.UnSubscribeAddressCode> unSubscribeAddress(@RequestBody SubscribeParam.UnSubscribeAddressInput input);
}
