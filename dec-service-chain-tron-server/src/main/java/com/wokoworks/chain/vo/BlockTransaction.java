package com.wokoworks.chain.vo;

import java.util.List;

import com.wokoworks.utils.NftUtils;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

@Data
public class BlockTransaction {
    private long blockNumber;
    private String blockHash;
    private long blockDt;
    private List<Transaction> transactionList;


    @Data
    public static class Transaction {
        private String unit;
        private String contractAddress;
        private String transactionHash;
        private String fromAddress;
        private String toAddress;
        private String methodId;
        private BigDecimal amount;
        private AssetType assetType;
        private BigDecimal netFee;
        private long netUsage;
        private BigDecimal energyFee;
        private long energyUsageTotal;
        private long energyUsage;
        private BigDecimal fee;
        private int decimals;


        private String eip = "";
        private BigInteger value;
        private String tokenUri;
        private String name;
        private NftUtils.NftInfo nftInfo;

        // for better cross chain recognition
        private Boolean crossChainTxnCheck;
        private String sourceChainName;
        private Integer sourceChainId;
        private String targetChainName;
        private Integer targetChainId;

        // store special info
        private String extraJson;


        /**
         * 是否已经完成了数据的处理(把查询货币金额跟货币单位的功能放到了业务之后减少了链上查询次数)
         */
        private boolean complete = false;

        public enum AssetType {
            MAIN, TRC10, TRIGGER_SMART_CONTRACT

        }
    }
}