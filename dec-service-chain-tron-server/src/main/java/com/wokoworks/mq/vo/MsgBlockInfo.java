package com.wokoworks.mq.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: 飞
 * @Date: 2021/11/12 11:53
 */
@Data
public class MsgBlockInfo {


    private String chainCode;
    private long blockNum;
    private String blockHash;
    private long blockDt;
    private List<Transaction> transactionList;


    @Data
    public static class Transaction {

        private String transactionHash;
        private BigDecimal amount;
        private BigDecimal fee;

        private String fromAddress;
        private String toAddress;
        private String symbol = "";
        private String contractAddress;
        private int decimals;
        private int tokenId;
        private String name;
        private boolean nft = false;
        private String file;
        private int fileType;
        private String eip;
    }


}
