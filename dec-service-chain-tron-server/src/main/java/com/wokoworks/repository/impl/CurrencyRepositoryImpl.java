// auto generator by wokoworks 2020年10月22日 下午5:46:52
package com.wokoworks.repository.impl;

import com.wokoworks.framework.data.impl.condition.Conditions;
import com.wokoworks.repository.CurrencyRepository;
import com.wokoworks.vo.Currency;
import com.wokoworks.framework.data.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * table name: currency
 *
 * @date 2020年10月22日 下午5:46:52
 * @author luobing
 */
@Repository
public class CurrencyRepositoryImpl extends BaseRepositoryImpl<Currency, Integer> implements CurrencyRepository {
    private static final String TABLE_NAME = "currency";

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public Class<Currency> getBeanClass() {
        return Currency.class;
    }

    @Override
    public Optional<Currency> findByUnit(String unit) {
        return select().where(Conditions.equals("unit", unit)).findOne();
    }

    @Override
    public List<Currency> selectInContractAddress(Collection<String> contractAddress) {
        if (contractAddress == null || contractAddress.isEmpty()) {
            return Collections.emptyList();
        }
        return select().where(Conditions.in("contract_address", contractAddress)).find();
    }

    @Override
    public Optional<Currency> findByContractAddress(String contractAddress) {
        return select()
                .where(Conditions.equals("contract_address", contractAddress))
                .findOne();
    }
}
