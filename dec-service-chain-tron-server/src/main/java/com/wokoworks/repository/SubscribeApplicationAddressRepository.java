// auto generator by wokoworks 2020年10月22日 下午5:46:52
package com.wokoworks.repository;

import com.wokoworks.vo.SubscribeApplicationAddress;
import com.wokoworks.framework.data.BaseRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * table name: subscribe_application_address
 *
 * @date 2020年10月22日 下午5:46:52
 * @author luobing
 */
public interface SubscribeApplicationAddressRepository extends BaseRepository<SubscribeApplicationAddress, Integer> {
    List<SubscribeApplicationAddress> selectByAppNumAndInSubscribeAddressIds(String appNum, Collection<Integer> subscribeAddressIds);

    List<SubscribeApplicationAddress> selectInSubscribeAddressIdsAndNotAppNum(Collection<Integer> subscribeAddressIds, String appNum);

    int deleteByAppNumAndInSubscribeAddressIds(String appNum, Collection<Integer> subscribeAddressId);

    List<SubscribeApplicationAddress> selectInSubscribeAddressIds(Collection<Integer> subscribeAddressIds);

    int batchInsertForUpdate(Collection<SubscribeApplicationAddress> subscribeApplicationAddresss);
}
