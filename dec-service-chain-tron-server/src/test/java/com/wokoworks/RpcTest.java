package com.wokoworks;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import com.wokoworks.chain.RpcClient;
import com.wokoworks.chain.vo.BlockTransaction;
import com.wokoworks.chain.vo.ContractToken;
import com.wokoworks.chain.vo.NewBlockHead;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.util.Lists;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.utils.Base58Check;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 飞
 * @Date: 2021/10/14 17:59
 */
@Slf4j
public class RpcTest {

    public static void main(String[] args) {
        RpcClient rpcClient = new RpcClient();
       // rpcClient.setClients(Lists.newArrayList(ApiWrapper.ofMainnet("")));
        rpcClient.setClients(Lists.newArrayList(ApiWrapper.ofNile("")));

        List<BlockTransaction.Transaction> blockTransactionByNumber = rpcClient.getBlockTransactionByNumber(	25470866);

        rpcClient.fillContractInfo(blockTransactionByNumber);

        System.out.println(JSON.toJSONString(blockTransactionByNumber, true));


    }







}
