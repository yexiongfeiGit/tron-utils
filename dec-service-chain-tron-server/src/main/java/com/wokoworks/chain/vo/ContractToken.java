package com.wokoworks.chain.vo;

import lombok.Data;

import java.math.BigInteger;

/**
 * @Author: 飞
 * @Date: 2021/10/13 12:06
 */
@Data
public class ContractToken {
    private String symbol;
    private BigInteger decimal;

    private Status status;

    private String name;
    private int eip;

    private boolean isNft = false;


    public enum Status {
        FAILED, SUCCESS
    }

}
