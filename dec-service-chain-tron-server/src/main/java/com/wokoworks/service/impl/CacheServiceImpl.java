package com.wokoworks.service.impl;
import com.google.protobuf.ByteString;
import com.wokoworks.chain.vo.ContractToken.Status;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wokoworks.chain.RpcClient;
import com.wokoworks.chain.vo.BlockTransaction;
import com.wokoworks.chain.vo.ContractToken;
import com.wokoworks.repository.CurrencyRepository;
import com.wokoworks.service.CacheService;
import com.wokoworks.vo.Currency;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.trident.utils.Base58Check;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 飞
 * @Date: 2021/10/13 11:50
 */
@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    @Autowired
    private RpcClient rpcClient;
    @Autowired
    private CurrencyRepository currencyRepository;


    private static final Cache<String, ContractToken> CACHE = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).softValues().build();



    @SneakyThrows
    @Override
    public ContractToken findContractToken(String contractAddress, BlockTransaction.Transaction.AssetType assetType) {


        ContractToken contractToken_ = CACHE.getIfPresent(contractAddress);
        if (contractToken_ != null) {
            return contractToken_;
        }


        Optional<Currency> currencyOptional = currencyRepository.findByContractAddress(contractAddress);
        if (currencyOptional.isPresent()) {
            Currency currency = currencyOptional.get();
            ContractToken contractToken = new ContractToken();
            contractToken.setSymbol(currency.getUnit());
            contractToken.setDecimal(BigInteger.valueOf(currency.getDecimals()));
            contractToken.setStatus(currency.getStatus() == 0 ? ContractToken.Status.FAILED : ContractToken.Status.SUCCESS);
            CACHE.put(contractAddress, contractToken);
            return contractToken;
        }


        /**
         * 去链上查询
         */
        ContractToken contractToken = new ContractToken();
        contractToken.setSymbol("");
        contractToken.setDecimal(BigInteger.valueOf(6));
        contractToken.setStatus(Status.SUCCESS);

        StringBuilder sb = new StringBuilder();


        /**
         * trc10
         */
        if (assetType == BlockTransaction.Transaction.AssetType.TRC10) {
            String symbol = rpcClient.getTrc10Symbol(contractAddress);
            log.warn("<=====1==========symbole:{}", symbol);
            if (symbol == null) {
                contractToken.setStatus(Status.FAILED);
                sb.append("symbol not found");
            }else {
                contractToken.setSymbol(symbol);
                CACHE.put(contractAddress, contractToken);
            }
        }



        if (assetType == BlockTransaction.Transaction.AssetType.TRIGGER_SMART_CONTRACT) {

            String symbol = rpcClient.getTrc20Symbol(contractAddress);
            log.warn("<=====2==========symbole:{}", symbol);
            if (Strings.isEmpty(symbol)) {
                contractToken.setStatus(Status.FAILED);
                sb.append("symbol not found ");
            } else {
                contractToken.setSymbol(symbol);
            }


            BigInteger decimal = rpcClient.getDecimal(contractAddress);
            if (decimal == null) {
                contractToken.setStatus(Status.FAILED);
                sb.append("decimal not found ");
            } else {
                contractToken.setDecimal(decimal);
                CACHE.put(contractAddress, contractToken);
            }


            /**
             * 判断是否为721
             */
            if (decimal == null) {

                ByteString byteString = ByteString.copyFrom(Base58Check.base58ToBytes(contractAddress));

                if(rpcClient.checkMethod(byteString)){

                    if (rpcClient.isTRC1155(byteString)) {
                        contractToken.setNft(true);
                        contractToken.setEip(1155);
                        String name = rpcClient.trc20Name(byteString);
                        contractToken.setName(name);
                    } else if (rpcClient.isTRC721(byteString)) {
                        contractToken.setNft(true);
                        contractToken.setEip(721);
                        String name = rpcClient.trc20Name(byteString);
                        contractToken.setName(name);
                    }
                }
            }

            CACHE.put(contractAddress, contractToken);
        }



        /**
         * 查询后保存并缓存数据,只缓存trc20的数据
         */
        if (!contractToken.isNft()) {
            Currency currency = new Currency();
            currency.setUnit(contractToken.getSymbol());
            currency.setContractAddress(contractAddress);
            currency.setDecimals(contractToken.getDecimal().intValue());
            currency.setDt(System.currentTimeMillis());
            currency.setStatus(contractToken.getStatus() == Status.SUCCESS ? 1: 0);
            currency.setRemark(sb.toString());
            currencyRepository.save(currency);
        }

        try {
        }catch (Exception e) {
            log.warn(e.getMessage(), e);
        }

        return contractToken;

    }




    @Override
    public void fillAmountAndDecimal(BlockTransaction.Transaction transaction) {

//        if (transaction.isComplete()) {
//            return;
//        }
        if (transaction.getAssetType() == BlockTransaction.Transaction.AssetType.TRC10) {
            ContractToken contractToken = this.findContractToken(transaction.getContractAddress(), transaction.getAssetType());
            if (contractToken.getStatus() == ContractToken.Status.FAILED) {
                log.warn("<==找不到货币单位:{}", transaction);
                transaction.setUnit("");
            }
            transaction.setUnit(contractToken.getSymbol());
            transaction.setDecimals(6);
        }

        if (transaction.getAssetType() == BlockTransaction.Transaction.AssetType.TRIGGER_SMART_CONTRACT) {

            ContractToken contractToken = this.findContractToken(transaction.getContractAddress(), BlockTransaction.Transaction.AssetType.TRIGGER_SMART_CONTRACT);
            if (contractToken.getStatus() == ContractToken.Status.FAILED) {
                log.warn("<==合约找不到币种或精度:{}", transaction);
                transaction.setUnit("");
                return;
            }
            //金额
            final BigDecimal amount = RpcClient.fromWei(transaction.getAmount(), contractToken.getDecimal().intValue());
            transaction.setAmount(amount);
            transaction.setUnit(contractToken.getSymbol());
            transaction.setDecimals(contractToken.getDecimal().intValue());

        }

       // transaction.setComplete(true);
    }






}
