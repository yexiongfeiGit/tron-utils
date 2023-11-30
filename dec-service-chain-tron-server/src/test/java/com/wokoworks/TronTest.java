package com.wokoworks;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.proto.Chain;

/**
 * @Author: 飞
 * @Date: 2021/9/27 09:56
 */
@SpringBootTest
public class TronTest {


    private static ApiWrapper createApiWrapper() {
        KeyPair keyPair = KeyPair.generate();
        String pri = keyPair.toPrivateKey().toString();
        return ApiWrapper.ofShasta(pri);
    }


    @Test
    public void test() throws IllegalException {

        Chain.Block nowBlock = createApiWrapper().getNowBlock();
        System.out.println(nowBlock.getBlockHeader().getRawData().getNumber());

        System.out.println("-----");
    }
}
