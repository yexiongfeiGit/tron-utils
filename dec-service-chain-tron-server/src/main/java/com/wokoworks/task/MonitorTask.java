package com.wokoworks.task;

import com.alibaba.fastjson.JSONObject;
import com.wokoworks.config.MonitorTaskProject;
import com.wokoworks.repository.BlockInfoRepository;
import com.wokoworks.utils.LarkNotificationUtils;
import com.wokoworks.vo.BlockInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * @author hjh
 * @Date 2023/3/8 下午4:49
 */
@Component
@Slf4j
@ConditionalOnBean(MonitorTaskProject.class)
public class MonitorTask {

    @Autowired
    private BlockInfoRepository blockInfoRepository;

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private MonitorTaskProject monitorTaskProject;

    private long height = -1;





    /**
     * 发现高度没有变化时，飞书告警
     */
    @Scheduled(fixedDelayString = "${monitor.monitor-height-time}")
    @Async
    public void monitorHeight(){
        if (!monitorTaskProject.isSchedule()){
            return;
        }
        log.info("monitorHeight  start,当前高度 height:{},monitorTaskProject:{}",height, JSONObject.toJSONString(monitorTaskProject));
        Optional<BlockInfo> blockInfoOptional = blockInfoRepository.findByIdDesc();

        if (!blockInfoOptional.isPresent()){
            //报警
            reportAnEmergency("获取不到区块");
            return;
        }
        long blockNumber = blockInfoOptional.get().getBlockNumber();
        //如果高度没更新告警，更新了则记录高度
        if (height == -1 || height != blockNumber){
            //记录高度
            height = blockNumber;
            log.info("monitorHeight  end,当前高度 height:{}",height);
        }else {
            //报警
            log.info("monitorHeight  end,当前高度 height:{}, dbHeight:{}",height,blockNumber);
            reportAnEmergency("区块高度不变，height:" + blockNumber);
        }

    }


    /**
     * 飞书报警
     * @param errorMsg 错误信息
     */
    public void reportAnEmergency(String errorMsg){
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        LarkNotificationUtils.callingLarkApi(monitorTaskProject.getFeishuCallUrl(),applicationName,applicationName,applicationName ,errorMsg,LarkNotificationUtils.WarningLevel.ERROR,date);
    }








}
