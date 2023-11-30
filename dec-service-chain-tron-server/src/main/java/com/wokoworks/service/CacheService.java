package com.wokoworks.service;

import com.wokoworks.chain.vo.BlockTransaction;
import com.wokoworks.chain.vo.ContractToken;
import org.tron.trident.core.ApiWrapper;

/**
 * @Author: 飞
 * @Date: 2021/10/13 11:50
 */
public interface CacheService {



    ContractToken findContractToken(String contractAddress, BlockTransaction.Transaction.AssetType assetType);


    /**
     * 给交易填充货币单位和货币精度
     * @param transaction
     */
    void fillAmountAndDecimal(BlockTransaction.Transaction transaction);

}
