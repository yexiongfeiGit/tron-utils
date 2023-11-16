package com.wokoworks.config;

import com.wokoworks.chain.helper.ChainIdChecker;
import com.wokoworks.chain.helper.ChainServerHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "chain")
public class ChainProperties {

    private String chainCode;

    private Integer chainId;

    private int deleteDays;

    private int retryCount;

    @PostConstruct
    public void postConstruct() throws Exception {

        ChainServerHelper.ChainServer chainServer = ChainIdChecker.isMainNet(chainId)
                ? ChainServerHelper.MAINNET_CHAIN_ID_SERVER_MAP.get(chainId) : ChainServerHelper.TESTNET_CHAIN_ID_SERVER_MAP.get(chainId);
        if (chainServer == null) {
            throw new Exception("Initialization Failed, Please correct your chainId config, " +
                    "or add chainServerName instance to ChainServerHelper on project dec-chain-constants");
        }
        chainCode = chainServer.getChainServerName();
        log.debug("<<< [ChainProperties] initial [{}] chain service", chainCode);
    }
}