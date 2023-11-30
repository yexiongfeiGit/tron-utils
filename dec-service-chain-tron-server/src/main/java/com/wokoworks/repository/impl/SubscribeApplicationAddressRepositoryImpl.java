// auto generator by wokoworks 2020年10月22日 下午5:46:52
package com.wokoworks.repository.impl;

import com.wokoworks.framework.data.impl.condition.Conditions;
import com.wokoworks.framework.data.impl.sqlbuilder.BatchGeneratorKeyConnectionCallback;
import com.wokoworks.repository.SubscribeApplicationAddressRepository;
import com.wokoworks.vo.SubscribeAddress;
import com.wokoworks.vo.SubscribeApplicationAddress;
import com.wokoworks.framework.data.impl.BaseRepositoryImpl;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * table name: subscribe_application_address
 *
 * @date 2020年10月22日 下午5:46:52
 * @author luobing
 */
@Repository
public class SubscribeApplicationAddressRepositoryImpl extends BaseRepositoryImpl<SubscribeApplicationAddress, Integer> implements SubscribeApplicationAddressRepository {
    private static final String TABLE_NAME = "subscribe_application_address";

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public Class<SubscribeApplicationAddress> getBeanClass() {
        return SubscribeApplicationAddress.class;
    }

    @Override
    public List<SubscribeApplicationAddress> selectByAppNumAndInSubscribeAddressIds(String appNum, Collection<Integer> subscribeAddressIds) {
        return select()
                .where(Conditions.equals("app_num", appNum))
                .where(Conditions.in("subscribe_address_id", subscribeAddressIds)).find();
    }

    @Override
    public List<SubscribeApplicationAddress> selectInSubscribeAddressIdsAndNotAppNum(Collection<Integer> subscribeAddressIds, String appNum) {
        return select()
                .where(Conditions.in("subscribe_address_id", subscribeAddressIds))
                .where(Conditions.raw("app_num<>?", new Object[]{ appNum }))
                .find();
    }

    @Override
    public int deleteByAppNumAndInSubscribeAddressIds(String appNum, Collection<Integer> subscribeAddressId) {
        return delete()
                .where(Conditions.equals("app_num", appNum))
                .where(Conditions.in("subscribe_address_id", subscribeAddressId)).delete();
    }

    @Override
    public List<SubscribeApplicationAddress> selectInSubscribeAddressIds(Collection<Integer> subscribeAddressIds) {
        if (subscribeAddressIds.isEmpty()) {
            return new ArrayList<>();
        }
        return select()
                .where(Conditions.in("subscribe_address_id", subscribeAddressIds)).find();
    }

    @Override
    public int batchInsertForUpdate(Collection<SubscribeApplicationAddress> subscribeApplicationAddresss) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO subscribe_application_address (app_num, subscribe_address_id, dt) VALUES ");
        boolean isFirst = true;
        for (SubscribeApplicationAddress item : subscribeApplicationAddresss) {
            if (isFirst) {
                sql.append("(?, ?, ?)");
                isFirst = false;
            } else {
                sql.append(",(?, ?, ?)");
            }
        }
        sql.append(" on duplicate key update dt=dt");
        final Integer result = super.getJdbcTemplate().execute(sql.toString(), new PreparedStatementCallback<Integer>(){
            @Override
            public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                int i = 1;
                for (SubscribeApplicationAddress item : subscribeApplicationAddresss) {
                    ps.setObject(i++, item.getAppNum());
                    ps.setObject(i++, item.getSubscribeAddressId());
                    ps.setObject(i++, item.getDt());
                }
                return ps.executeUpdate();
            }
        });
        return result == null? 0: result;
    }
}
