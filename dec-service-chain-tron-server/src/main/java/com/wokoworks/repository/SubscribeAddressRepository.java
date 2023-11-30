// auto generator by wokoworks 2020年10月22日 下午5:46:52
package com.wokoworks.repository;

import com.wokoworks.framework.data.BaseRepository;
import com.wokoworks.vo.SubscribeAddress;
import org.springframework.jdbc.support.KeyHolder;

import java.util.Collection;
import java.util.List;

/**
 * table name: subscribe_address
 *
 * @date 2020年10月22日 下午5:46:52
 * @author luobing
 */
public interface SubscribeAddressRepository extends BaseRepository<SubscribeAddress, Integer> {
    List<SubscribeAddress> selectInAddress(Collection<String> address);

    int deleteInId(Collection<Integer> deleteSubscribeAddressIds);

    int[] batchSaveForUpdate(Collection<SubscribeAddress> subscribeAddresss, KeyHolder keyHolder);
}
