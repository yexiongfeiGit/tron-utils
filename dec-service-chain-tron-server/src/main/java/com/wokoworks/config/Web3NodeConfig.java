package com.wokoworks.config;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * Web3 Nodes config
 *
 * @author Roylic
 * 2023/6/30
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "web3.nodes")
public class Web3NodeConfig {

    @Autowired
    private ChainProperties chainProperties;

    private String gtwyKey;

    private Map<Integer, List<NodeInfo>> web3NodeMap;

    // filled by reading the configuration file
    private List<NodeInfo> curChainNodes;


    @Data
    public static class NodeInfo {
        private String nodeName;
        private String wsPath;
        private String jsonRpcPath;
        private String grpcPath;
        private Integer grpcPort;
        private boolean ssl;
        private boolean indirectly = false;
    }

    public void setWeb3NodeMap(Map<Integer, List<NodeInfo>> web3NodeMap) {
        this.web3NodeMap = web3NodeMap;
        this.curChainNodes = web3NodeMap.get(chainProperties.getChainId());
    }

    public List<NodeInfo> getAllAvailableNodeOnThisChain() {
        return this.curChainNodes;
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("<<< [Web3NodeConfig] got nodes info:{}", JSON.toJSONString(web3NodeMap));
    }

}
