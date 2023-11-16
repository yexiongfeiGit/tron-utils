// auto generator by wokoworks 2020年10月22日 下午5:46:52
package com.wokoworks.repository;

import com.wokoworks.framework.data.BaseRepository;
import com.wokoworks.vo.Currency;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * table name: currency
 *
 * @date 2020年10月22日 下午5:46:52
 * @author luobing
 */
public interface CurrencyRepository extends BaseRepository<Currency, Integer> {
    Optional<Currency> findByUnit(String unit);

    List<Currency> selectInContractAddress(Collection<String> contractAddress);

    Optional<Currency> findByContractAddress(String contractAddress);
}
