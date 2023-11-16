// auto generator by wokoworks 2020年10月22日 下午5:46:52
package com.wokoworks.repository.impl;

import com.wokoworks.framework.data.impl.BaseRepositoryImpl;
import com.wokoworks.framework.data.impl.condition.Conditions;
import com.wokoworks.framework.data.impl.sqlbuilder.BatchGeneratorKeyConnectionCallback;
import com.wokoworks.repository.SubscribeAddressRepository;
import com.wokoworks.vo.SubscribeAddress;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * table name: subscribe_address
 *
 * @date 2020年10月22日 下午5:46:52
 * @author luobing
 */
@Repository
public class SubscribeAddressRepositoryImpl extends BaseRepositoryImpl<SubscribeAddress, Integer> implements SubscribeAddressRepository {
    private static final String TABLE_NAME = "subscribe_address";

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public Class<SubscribeAddress> getBeanClass() {
        return SubscribeAddress.class;
    }

    @Override
    public List<SubscribeAddress> selectInAddress(Collection<String> address) {
        return select().where(Conditions.in("address", address)).find();
    }

    @Override
    public int deleteInId(Collection<Integer> deleteSubscribeAddressIds) {
        return delete().where(Conditions.in("id", deleteSubscribeAddressIds)).delete();
    }

    @Override
    public int[] batchSaveForUpdate(Collection<SubscribeAddress> subscribeAddresss, KeyHolder keyHolder) {
        String sql = "INSERT INTO subscribe_address(address, dt) VALUES (?, ?) on duplicate key update dt = dt+1";
        List<Object[]> argList = new ArrayList<>(subscribeAddresss.size());
        for (SubscribeAddress item : subscribeAddresss) {
            Object[] args = new Object[2];
            args[0] = item.getAddress();
            args[1] = item.getDt();
            argList.add(args);
        }
        return super.getJdbcTemplate().execute(new BatchGeneratorKeyConnectionCallback(sql, argList, keyHolder));
    }
}
