package com.wokoworks.nft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wokoworks.utils.NftUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Roylic
 * 2023/2/13
 */
public class TokenURIParseTest {

    String tokenUri = "ipfs://QmYWTEdoM3noH5PxMFKdzCtxbooAerXcC3yDKuJggVLBX9/1";

    @Test
    public void testOpenSeaBase64NftTest() throws JsonProcessingException {
        NftUtils.NftInfo nftInfo = NftUtils.parseNft(tokenUri);
        ObjectMapper om = new ObjectMapper();
        System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(nftInfo));
    }
}
