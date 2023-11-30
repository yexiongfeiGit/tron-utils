package com.wokoworks.web3j;

import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class Balancer {
    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private NacosRegistration registration;

    private final AtomicReference<BalanceNodeChoose> nodeChoose = new AtomicReference<>();

    private void createChoose() {
        final BalanceNodeChoose chooser = new BalanceNodeChoose();
        final List<ServiceInstance> services = discoveryClient.getInstances(registration.getServiceId());
        for (ServiceInstance service : services) {
            chooser.addNode(service);
        }
        nodeChoose.set(chooser);
    }

    @PostConstruct
    public void init() throws NacosException {

        // 主动创建一次
        createChoose();

        // 监听服务上下线通知, 当节点上下线时主动重新创建负载器
        final NamingService namingService = registration.getNacosDiscoveryProperties().namingServiceInstance();
        namingService.subscribe(registration.getServiceId(), new EventListener() {
            @Override
            public void onEvent(Event event) {
                if (event instanceof NamingEvent) {
                    final NamingEvent namingEvent = (NamingEvent) event;
                    final String instanceId = registration.getInstanceId();
                    if (namingEvent.getInstances().stream().map(Instance::getInstanceId).anyMatch(id -> id.equals(instanceId))) {
                        createChoose();
                    }
                }
            }
        });
    }

    public static class BalanceNodeChoose {
        private final TreeMap<Long, ServiceInstance> nodeMap = new TreeMap<>(Long::compare);
        private final int virtualNodeCount;

        public BalanceNodeChoose() {
            this(10);
        }

        public BalanceNodeChoose(int virtualNodeCount) {
            this.virtualNodeCount = virtualNodeCount;
        }

        public void addNode(ServiceInstance node) {
            addNode(node, virtualNodeCount);
        }

        public long hashNode(ServiceInstance node) {
            return hash(node.getInstanceId());
        }

        public void addNode(ServiceInstance node, int virtualNodeCount) {
            for (int i = 0; i < virtualNodeCount; i++) {
                nodeMap.put(hashNode(node), node);
            }
        }

        //        public void removeNode(ServiceInstance node) {
//            removeNode(node, virtualNodeCount);
//        }
//
//        public void removeNode(ServiceInstance node, int virtualNodeCount) {
//            for (int i = 0; i < virtualNodeCount; i++) {
//                nodeMap.remove(hashNode(node));
//            }
//        }
        private final HashFunction hashFunction = Hashing.sha256();

        private long hash(String key) {
            return Math.abs(hashFunction.hashBytes(key.getBytes()).asLong());
        }

        private long hash(int id) {
            return Math.abs(hashFunction.hashInt(id).asLong());
        }

        public ServiceInstance getNode(int id) {
            final long hash = hash(id);
            Map.Entry<Long, ServiceInstance> entry = nodeMap.ceilingEntry(hash);
            if (entry == null) {
                entry = nodeMap.ceilingEntry(0L);
            }
            return entry.getValue();
        }
    }

    public boolean needProcess(int id) {
        final BalanceNodeChoose chooser = nodeChoose.get();
        final ServiceInstance instance = chooser.getNode(id);
        return instance.getInstanceId().equals(registration.getInstanceId());
    }

}
