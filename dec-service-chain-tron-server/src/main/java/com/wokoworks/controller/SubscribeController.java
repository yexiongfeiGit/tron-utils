package com.wokoworks.controller;

import com.wokoworks.chain.tron.common.codes.codes.SubscribeCode;
import com.wokoworks.chain.tron.common.codes.params.SubscribeParam;
import com.wokoworks.framework.commons.data.CallValue;
import com.wokoworks.framework.data.ReturnValue;
import com.wokoworks.service.SubscribeService;
import io.micrometer.core.instrument.Metrics;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/subscribe")
public class SubscribeController {

    @Autowired
    private SubscribeService subscribeService;

    @PostMapping("/subscribeAddress")
    @ApiOperation("订阅地址")
    public CallValue<Void, SubscribeCode.SubscribeAddressCode> subscribeAddress(@Valid @RequestBody SubscribeParam.SubscribeAddressInput input) {
        log.debug("==> 订阅地址input: {}", input);
        final String appNum = input.getAppNum();
        final Set<String> addressSet = input.getAddressSet();
        ReturnValue<Void, SubscribeCode.SubscribeAddressCode> returnValue = subscribeService.subscribeAddress(appNum, addressSet);
        if (returnValue.hasError()) {
            return CallValue.callError(returnValue.getError());
        }
        log.debug("<== 订阅地址结束");
        return CallValue.callOk(returnValue.getData());
    }

    @PostMapping("/unSubscribeAddress")
    @ApiOperation("取消订阅地址")
    public CallValue<Void, SubscribeCode.UnSubscribeAddressCode> unSubscribeAddress(@Valid @RequestBody SubscribeParam.UnSubscribeAddressInput input) {
        log.debug("==> 取消订阅地址input: {}", input);
        final String appNum = input.getAppNum();
        final Set<String> addressSet = input.getAddressSet();
        ReturnValue<Void, SubscribeCode.UnSubscribeAddressCode> returnValue = subscribeService.unSubscribeAddress(appNum, addressSet);
        if (returnValue.hasError()) {
            return CallValue.callError(returnValue.getError());
        }
        log.debug("<== 取消订阅地址结束");
        return CallValue.callOk(returnValue.getData());
    }


//    @GetMapping("/test")
//    public String test() {
//        final AtomicInteger atomicInteger = MetricsMonitor.test2(new AtomicInteger(), "node1");
//        atomicInteger.set(200);
//
//        final AtomicInteger atomicInteger1 = MetricsMonitor.test2(new AtomicInteger(), "node2");
//        atomicInteger1.set(400);
//
//        final AtomicInteger atomicInteger2 = MetricsMonitor.test2(new AtomicInteger(), "node3");
//        atomicInteger2.set(200);
//        return "ok";
//    }
}
