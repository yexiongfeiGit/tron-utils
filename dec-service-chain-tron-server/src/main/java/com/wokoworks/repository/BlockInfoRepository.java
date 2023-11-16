// auto generator by wokoworks 2020年7月29日 下午6:46:26
package com.wokoworks.repository;

import com.wokoworks.framework.data.BaseRepository;
import com.wokoworks.vo.BlockInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * table name: block_info
 *
 * @date 2020年7月29日 下午6:46:26
 * @author luobing
 */
public interface BlockInfoRepository extends BaseRepository<BlockInfo, Integer> {
    Optional<BlockInfo> findLast();

    Optional<BlockInfo> findByBlockNumber(long blockNumber);

    //int updateBlockHashAndParentHashAndNodeNameAndStatusAndForkDtByBlockNumber(long blockNumber, String blockHash, String parentHash, String nodeName, BlockInfo.Status status, long forkDt, long blockDt);

    int updateBlockHashAndParentHashAndNodeNameAndStatusAndForkDtByIdAndOldBlockHash(int id, String oldBlockHash, String blockHash, String parentHash, String nodeName, BlockInfo.Status status, long forkDt, long blockDt);

    int saveOrUpdate(BlockInfo blockInfo);

    int updateStatusUpdateDtByBlockNumber(long blockNumber, BlockInfo.Status updateStatus, long updateDt);

    List<BlockInfo> findByInStatussOrderAsc(Collection<Short> statuss, int serviceCount, int serviceIndex, int pageSize, int retryCount);

    List<BlockInfo> findByInStatussOrderDesc(Collection<Short> statuss, int serviceCount, int serviceIndex, int pageSize, int retryCount);

    List<BlockInfo> findByInStatussOrderAsc(Collection<Short> statuss, int pageSize, int retryCount);

    int updateStatusFailPushCountUpdateDtByBlockNumberAndStatuss(long blockNumber, Collection<Short> whereStatuss, BlockInfo.Status updateStatus, int increaseFailPushCount, long updateDt);

    int updateStatusUpdateDtByIdAndStatuss(int id, Collection<Short> whereStatuss, BlockInfo.Status updateStatus, long updateDt);

    int findCountByStatusAndLessThanEqualsUpdateDt(BlockInfo.Status status, long readyProcessDt);

    int updateStatusByStatusAndLessThenEqualsUpdateDt(BlockInfo.Status whereStatus, long updateDt, BlockInfo.Status updateStatus);

    int findCountByLessThanEqualsForkDtAndGreaterThenFailPushCount(long forkDt, long failPushCount);

    int updateStatusByStatussAndLessThanEqualsForkDtAndGreaterThenFailPushCount(Collection<Short> whereStatuss, long forkDt, long failPushCount, BlockInfo.Status updateStatus);

    Integer findByLimitStartRow(int startRow);

    int deleteByLessThenEqualsIdLimitDeleteRows(int id, int deleteRows);

    int findLessThanDt(long dt);

    Optional<BlockInfo> findByIdDesc();
}
