package com.wokoworks.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


/**
 * @author hjh
 * @Date 2023/3/9 上午11:33
 */
@Data
@ConfigurationProperties(prefix = "monitor")
@Configuration
@ConditionalOnProperty(name = "monitor.schedule",havingValue = "true")
public class MonitorTaskProject {

    private boolean schedule;
    private String monitorHeightTime;
    private String feishuCallUrl;

}
