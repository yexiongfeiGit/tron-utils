package com.wokoworks.utils;

import com.google.common.collect.Lists;
import io.functionx.consumetask.BatchConsumeTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.util.List;

@Slf4j
public abstract class AbstractListTaskHandler<T> implements BatchConsumeTask.BatchTaskHandler<T> {
    private DiscoveryClient discoveryClient;// 服务发现
    private Registration registration; // 服务注册
    public AbstractListTaskHandler(BeanFactory beanFactory) {
        this.discoveryClient = beanFactory.getBean(DiscoveryClient.class);
        this.registration = beanFactory.getBean(Registration.class);
    }
    public List<T> searchTask() {
        final List<ServiceInstance> instanceList = discoveryClient.getInstances(registration.getServiceId());
        int serviceIndex = 0;
        int totalCount = instanceList.size();
        for (int i = 0; i < totalCount; i++) {
            final ServiceInstance serviceInstance = instanceList.get(i);
            if (serviceInstance.getHost().equals(registration.getHost()) && serviceInstance.getPort() == registration.getPort()) {
                serviceIndex = i;
                break;
            }
        }
        if (totalCount == 0) {
            totalCount = 1;
        }
        try {
            return searchTask(totalCount, serviceIndex);
        } catch (Throwable ex) {
            log.warn("abstractListTaskHandler 查询任务异常", ex);
            return Lists.newArrayList();
        }
    }
    public abstract List<T> searchTask(int serviceCount, int serviceIndex);
    public abstract void handleTask(T data);
}