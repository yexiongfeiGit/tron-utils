package com.wokoworks.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dec-chain-tron-schedule")
@EnableConfigurationProperties(ScheduleProperties.class)
public class ScheduleProperties {
	/**
	 * 调度系统备注
	 */
	private String scheduleSystem;

	/**
	 * 系统私钥
	 */
	private String priKey;
}
