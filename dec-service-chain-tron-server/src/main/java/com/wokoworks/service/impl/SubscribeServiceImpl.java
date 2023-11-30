package com.wokoworks.service.impl;

import com.wokoworks.chain.tron.common.codes.codes.SubscribeCode;
import com.wokoworks.framework.data.ReturnValue;
import com.wokoworks.repository.SubscribeAddressRepository;
import com.wokoworks.repository.SubscribeApplicationAddressRepository;
import com.wokoworks.service.SubscribeService;
import com.wokoworks.vo.SubscribeAddress;
import com.wokoworks.vo.SubscribeApplicationAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubscribeServiceImpl implements SubscribeService {

    @Autowired
    private SubscribeAddressRepository subscribeAddressRepository;
    @Autowired
    private SubscribeApplicationAddressRepository subscribeApplicationAddressRepository;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Override
    public ReturnValue<Void, SubscribeCode.SubscribeAddressCode> subscribeAddress(String appNum, Set<String> addressSet) {
        log.debug("==> appNum: {}, addressList: {}", appNum, addressSet);
        if (addressSet.isEmpty()) {
            log.info("<== 没有订阅的地址 appNum:{}", appNum);
            return ReturnValue.withOk(null);
        }

        final long now = System.currentTimeMillis();
        // 要insert的地址
        List<SubscribeAddress> insertSubscribeAddressList = new ArrayList<>();
        // 要insert的项目关联地址
        List<SubscribeApplicationAddress> insertSubscribeApplicationAddressList = new ArrayList<>();
        // 查询出已存在地址
        final List<SubscribeAddress> subscribeAddressList = subscribeAddressRepository.selectInAddress(addressSet);
        if (subscribeAddressList.isEmpty()) {
            for (String address : addressSet) {
                SubscribeAddress subscribeAddress = new SubscribeAddress();
                subscribeAddress.setAddress(address);
                subscribeAddress.setDt(now);

                insertSubscribeAddressList.add(subscribeAddress);
            }
        } else {

            final Map<String, SubscribeAddress> subscribeAddressMap = subscribeAddressList.stream().collect(Collectors.toMap(SubscribeAddress::getAddress, Function.identity()));
            final Set<Integer> subscribeAddressIdSet = subscribeAddressList.stream().map(SubscribeAddress::getId).collect(Collectors.toSet());

            // 查询出这个appNum下已存在地址
            final List<SubscribeApplicationAddress> subscribeApplicationAddressList = subscribeApplicationAddressRepository.selectByAppNumAndInSubscribeAddressIds(appNum, subscribeAddressIdSet);
            final Map<Integer, SubscribeApplicationAddress> subscribeApplicationAddressMap = subscribeApplicationAddressList.stream().collect(Collectors.toMap(SubscribeApplicationAddress::getSubscribeAddressId, Function.identity()));

            for (String address : addressSet) {
                if (!subscribeAddressMap.containsKey(address)) {
                    //不存在的地址添加
                    SubscribeAddress subscribeAddress = new SubscribeAddress();
                    subscribeAddress.setAddress(address);
                    subscribeAddress.setDt(now);

                    insertSubscribeAddressList.add(subscribeAddress);
                } else {
                    final SubscribeAddress subscribeAddress = subscribeAddressMap.get(address);
                    //存在的地址看看关系表是否存在
                    SubscribeApplicationAddress subscribeApplicationAddress = subscribeApplicationAddressMap.get(subscribeAddress.getId());
                    if (subscribeApplicationAddress == null) {
                        //存在的地址，关系表中不存在，添加关系表
                        subscribeApplicationAddress = new SubscribeApplicationAddress();
                        subscribeApplicationAddress.setAppNum(appNum);
                        subscribeApplicationAddress.setSubscribeAddressId(subscribeAddress.getId());
                        subscribeApplicationAddress.setDt(now);

                        insertSubscribeApplicationAddressList.add(subscribeApplicationAddress);
                    }
                }
            }
        }

        if (!insertSubscribeAddressList.isEmpty()) {
            TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
            Boolean result = transactionTemplate.execute(status -> {

                KeyHolder keyHolder = new GeneratedKeyHolder();
                subscribeAddressRepository.batchSaveForUpdate(insertSubscribeAddressList, keyHolder);
                // 设置id到结果里
                subscribeAddressRepository.setIds(insertSubscribeAddressList, keyHolder, SubscribeAddress::setId, number -> number.intValue());

                for (SubscribeAddress item : insertSubscribeAddressList) {
                    SubscribeApplicationAddress subscribeApplicationAddress = new SubscribeApplicationAddress();
                    subscribeApplicationAddress.setAppNum(appNum);
                    subscribeApplicationAddress.setSubscribeAddressId(item.getId());
                    subscribeApplicationAddress.setDt(now);

                    insertSubscribeApplicationAddressList.add(subscribeApplicationAddress);
                }

                subscribeApplicationAddressRepository.batchInsertForUpdate(insertSubscribeApplicationAddressList);
                return true;
            });
            if (result == null || !result) {
                return ReturnValue.withError(SubscribeCode.SubscribeAddressCode.SAVE_ERROR);
            }
        } else if (!insertSubscribeApplicationAddressList.isEmpty()) {
            subscribeApplicationAddressRepository.batchInsertForUpdate(insertSubscribeApplicationAddressList);
        }

        log.debug("<==");
        return ReturnValue.withOk(null);
    }

    @Override
    public ReturnValue<Void, SubscribeCode.UnSubscribeAddressCode> unSubscribeAddress(String appNum, Set<String> addressSet) {
        log.debug("==> appNum: {}, addressList: {}", appNum, addressSet);
        if (addressSet.isEmpty()) {
            log.info("<== 没有取消订阅的地址 appNum:{}", appNum);
            return ReturnValue.withOk(null);
        }

        // 查询出要删除的地址
        final List<SubscribeAddress> subscribeAddressList = subscribeAddressRepository.selectInAddress(addressSet);
        if (subscribeAddressList.isEmpty()) {
            log.info("<== 取消订阅地址不存在 appNum:{}, addressList:{}", appNum, addressSet);
            return ReturnValue.withError(SubscribeCode.UnSubscribeAddressCode.ADDRESS_NOT_EXIST);
        }
        Set<Integer> subscribeAddressSet = subscribeAddressList.stream().map(SubscribeAddress::getId).collect(Collectors.toSet());

        //记录可以删除的地址id
        Collection<Integer> deleteSubscribeAddressIdList;
        // 查询出地址被其它appNum关联过数据, 这些地址不能删
        List<SubscribeApplicationAddress> subscribeApplicationAddressList = subscribeApplicationAddressRepository.selectInSubscribeAddressIdsAndNotAppNum(subscribeAddressSet, appNum);
        if (subscribeApplicationAddressList.isEmpty()) {
            deleteSubscribeAddressIdList = subscribeAddressSet;
        } else {
            Set<Integer> notDeleteSubscribeAddressSet = subscribeApplicationAddressList.stream().map(SubscribeApplicationAddress::getSubscribeAddressId).collect(Collectors.toSet());

            deleteSubscribeAddressIdList = new ArrayList<>();
            for (Integer item : subscribeAddressSet) {
                if (!notDeleteSubscribeAddressSet.contains(item)) {
                    deleteSubscribeAddressIdList.add(item);
                }
            }
        }

        if (deleteSubscribeAddressIdList.isEmpty()) {
            //删除系统和地址关系表
            subscribeApplicationAddressRepository.deleteByAppNumAndInSubscribeAddressIds(appNum, subscribeAddressSet);
        } else {
            TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
            Boolean result = transactionTemplate.execute(status -> {
                //删除地址
                subscribeAddressRepository.deleteInId(deleteSubscribeAddressIdList);
                //删除系统和地址关系表
                subscribeApplicationAddressRepository.deleteByAppNumAndInSubscribeAddressIds(appNum, subscribeAddressSet);
                return true;
            });
            if (result == null || !result) {
                return ReturnValue.withError(SubscribeCode.UnSubscribeAddressCode.SAVE_ERROR);
            }
        }

        log.debug("<==");
        return ReturnValue.withOk(null);
    }
}
