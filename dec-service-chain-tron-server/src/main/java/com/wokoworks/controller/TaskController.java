package com.wokoworks.controller;

import com.google.common.collect.Maps;
import com.wokoworks.chain.RpcClient;
import com.wokoworks.chain.vo.NewBlockHead;
import com.wokoworks.config.ChainProperties;
import com.wokoworks.config.ScheduleProperties;
import com.wokoworks.framework.commons.data.CallValue;
import com.wokoworks.framework.commons.utils.SignUtils;
import com.wokoworks.repository.BlockInfoRepository;
import com.wokoworks.service.impl.DispatchTransactionService;
import com.wokoworks.service.impl.HandleNewHeadService;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private BlockInfoRepository blockInfoRepository;
    @Autowired
    private ScheduleProperties scheduleProperties;
    @Autowired
    private DispatchTransactionService dispatchTransactionService;
    @Autowired
    private ChainProperties chainProperties;


    @Autowired
    private RpcClient rpcClient;
    @Autowired
    private HandleNewHeadService handleNewHeadService;


    private static volatile long lastBlockNum = 0;


    /**
     * 区块扫描
     *
     * @return
     */
    @PostMapping("/scan")
    //@Scheduled(cron = "0/5 * * * * ?")
    public CallValue<Void, Enum> initScaner(@RequestBody @Valid TaskInput input) {

        if (!verifySchedule(input)) {
            return CallValue.callError(ScheduleCode.VERIFY_FAIL);
        }

        while (true) {
            NewBlockHead newBlockHead = rpcClient.getBlockHeader();
            log.debug("接收 newBlockHead: {}, server-dt:{}", newBlockHead, Instant.now().toEpochMilli());

            if (newBlockHead.getBlockNumber() > lastBlockNum) {
                lastBlockNum = newBlockHead.getBlockNumber();
                handleNewHeadService.handleBlock(newBlockHead);
                continue;
            }
            break;
        }
        return CallValue.callOk(null);
    }

    @PostMapping("/clearBlockInfoHistory")
    @ApiOperation("定时清理历史数据")
    public CallValue<Void, Enum> clearBlockInfoHistory(@RequestBody @Valid TaskInput input) {
        if (!verifySchedule(input)) {
            return CallValue.callError(ScheduleCode.VERIFY_FAIL);
        }

        int deleteCount = blockInfoRepository.findLessThanDt(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(chainProperties.getDeleteDays()));
        log.info("<==定期删除旧数据,删除{}天之前数据, 条数:{}", chainProperties.getDeleteDays(), deleteCount);
        return CallValue.callOk(null);
    }


    private boolean verifySchedule(TaskController.TaskInput input) {
        String sign = input.getSign();
        String timestamp = input.getTimestamp();

        Map<String, String[]> map = Maps.newHashMap();
        map.put("timestamp", new String[]{timestamp});
        boolean result;
        try {
            result = SignUtils.verify(map, scheduleProperties.getScheduleSystem(), sign, scheduleProperties.getPriKey());
        } catch (Exception ex) {
            log.info("疑似刷接口,调度签名异常 error:{}", ex.getMessage());
            return false;
        }
        if (!result) {
            log.info("疑似刷接口,调度签名异常");
            return false;
        }
        return true;
    }

    public enum ScheduleCode {
        VERIFY_FAIL("验证失败");

        ScheduleCode(String msg) {
        }
    }

    @Data
    public static class TaskInput {
        @NotBlank
        @ApiModelProperty("签名")
        private String sign;
        @NotBlank
        @ApiModelProperty("时间戳")
        private String timestamp;
    }

}
