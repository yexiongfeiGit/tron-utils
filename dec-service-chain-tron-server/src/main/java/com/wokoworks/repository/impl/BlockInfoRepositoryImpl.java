// auto generator by wokoworks 2020年7月29日 下午6:46:26
package com.wokoworks.repository.impl;

import com.wokoworks.framework.data.Sort;
import com.wokoworks.framework.data.impl.BaseRepositoryImpl;
import com.wokoworks.framework.data.impl.condition.Conditions;
import com.wokoworks.framework.data.impl.sqlbuilder.SelectBuilder;
import com.wokoworks.repository.BlockInfoRepository;
import com.wokoworks.vo.BlockInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * table name: block_info
 *
 * @date 2020年7月29日 下午6:46:26
 * @author luobing
 */
@Slf4j
@Repository
public class BlockInfoRepositoryImpl extends BaseRepositoryImpl<BlockInfo, Integer> implements BlockInfoRepository {
    private static final String TABLE_NAME = "block_info";

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public Class<BlockInfo> getBeanClass() {
        return BlockInfo.class;
    }

    @Override
    public Optional<BlockInfo> findLast() {
        return select().orderBy(new Sort("block_number", Sort.Direction.DESC)).findOne();
    }

    @Override
    public Optional<BlockInfo> findByBlockNumber(long blockNumber) {
        return select().where(Conditions.equals("block_number", blockNumber)).findOne();
    }

//    @Override
//    public int updateBlockHashAndParentHashAndNodeNameAndStatusAndForkDtByBlockNumber(long blockNumber, String blockHash, String parentHash, String nodeName, BlockInfo.Status status, long forkDt, long blockDt) {
//        StringBuilder sql = new StringBuilder();
//        sql.append("update block_info a");
//        sql.append(" inner join block_info b on b.block_number=a.block_number-1 and a.parent_hash=b.block_hash");
//        sql.append(" set a.block_hash=?,a.parent_hash=?,a.node_name=?,a.status=?,a.fork_dt=?,a.block_dt=?");
//        sql.append(" where a.block_number=?");
//        return super.getJdbcTemplate().update(sql.toString(), blockHash, parentHash, nodeName, status.value, forkDt, blockDt, blockNumber);
//    }

    @Override
    public int updateBlockHashAndParentHashAndNodeNameAndStatusAndForkDtByIdAndOldBlockHash(int id, String oldBlockHash, String blockHash, String parentHash, String nodeName, BlockInfo.Status status, long forkDt, long blockDt) {
        return update().set("block_hash", blockHash)
                .set("parent_hash", parentHash)
                .set("node_name", nodeName)
                .set("status", status.value)
                .set("fork_dt", forkDt)
                .set("block_dt", blockDt)
                .where(Conditions.equals("id", id))
                .where(Conditions.equals("block_hash", oldBlockHash))
                .update();
    }

    @Override
    public int saveOrUpdate(BlockInfo blockInfo) {
        final String sql = "INSERT INTO " + TABLE_NAME + "(block_hash, block_number, parent_hash, node_name, status, seize_count, fail_push_count, fork_dt, update_dt, block_dt, dt)" +
                " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) on duplicate key update seize_count = seize_count + 1";
        return update(sql, blockInfo.getBlockHash(), blockInfo.getBlockNumber(), blockInfo.getParentHash(), blockInfo.getNodeName(),
                blockInfo.getStatus(), 1, blockInfo.getFailPushCount(), blockInfo.getForkDt(), blockInfo.getUpdateDt(), blockInfo.getBlockDt(), blockInfo.getDt());
    }

    @Override
    public int updateStatusUpdateDtByBlockNumber(long blockNumber, BlockInfo.Status updateStatus, long updateDt) {
        return update().set("status", updateStatus.value)
                .set("update_dt", updateDt)
                .where(Conditions.equals("block_number", blockNumber))
                .update();
    }

    @Override
    public List<BlockInfo> findByInStatussOrderAsc(Collection<Short> statuss, int serviceCount, int serviceIndex, int pageSize, int retryCount) {
        SelectBuilder<BlockInfo> select = select();
        select.where(Conditions.in("status", statuss));
        select.where(Conditions.raw("block_number%?=?", new Object[]{serviceCount, serviceIndex}));
        select.where(Conditions.lessThen("fail_push_count", retryCount));
        return select.orderBy(Sort.of("id", Sort.Direction.ASC)).limit(pageSize).find();
    }

    @Override
    public List<BlockInfo> findByInStatussOrderAsc(Collection<Short> statuss, int pageSize, int retryCount) {
        SelectBuilder<BlockInfo> select = select();
        select.where(Conditions.in("status", statuss));
        select.where(Conditions.lessThen("fail_push_count", retryCount));
        return select.orderBy(Sort.of("id", Sort.Direction.ASC)).limit(pageSize).find();
    }

    @Override
    public List<BlockInfo> findByInStatussOrderDesc(Collection<Short> statuss, int serviceCount, int serviceIndex, int pageSize, int retryCount) {
        log.debug("==>statuss: {}, serviceCount: {}, serviceIndex: {}, pageSize: {}, retryCount: {}", statuss, serviceCount, serviceIndex, pageSize, retryCount);
        SelectBuilder<BlockInfo> select = select();
        select.where(Conditions.in("status", statuss));
        select.where(Conditions.raw("block_number%?=?", new Object[]{serviceCount, serviceIndex}));
        select.where(Conditions.lessThen("fail_push_count", retryCount));
        return select.orderBy(Sort.of("id", Sort.Direction.DESC)).limit(pageSize).find();
    }

    @Override
    public int updateStatusFailPushCountUpdateDtByBlockNumberAndStatuss(long blockNumber, Collection<Short> whereStatuss, BlockInfo.Status updateStatus, int increaseFailPushCount, long updateDt) {
       log.info("==>blockNumber: {}, whereStatuss: {}, updateStatus: {}, increaseFailPushCount: {}, updateDt: {}", blockNumber, whereStatuss, updateStatus, increaseFailPushCount, updateDt);
        return update().set("status", updateStatus.value)
                .set("update_dt", updateDt)
                .setRawField("fail_push_count = fail_push_count + ?", increaseFailPushCount)
                .where(Conditions.equals("block_number", blockNumber))
                .where(Conditions.in("status", whereStatuss))
                .update();
    }

    @Override
    public int updateStatusUpdateDtByIdAndStatuss(int id, Collection<Short> whereStatuss, BlockInfo.Status updateStatus, long updateDt) {
        return update().set("status", updateStatus.value)
                .set("update_dt", updateDt)
                .where(Conditions.equals("id", id))
                .where(Conditions.in("status", whereStatuss))
                .update();
    }

    @Override
    public int findCountByStatusAndLessThanEqualsUpdateDt(BlockInfo.Status status, long updateDt) {
        return select().where(Conditions.equals("status", status.value).and(Conditions.lessThanEquals("update_dt", updateDt))).findCount();
    }

    @Override
    public int updateStatusByStatusAndLessThenEqualsUpdateDt(BlockInfo.Status whereStatus, long updateDt, BlockInfo.Status updateStatus) {
        return update().set("status", updateStatus.value)
                .where(Conditions.equals("status", whereStatus.value).and(Conditions.lessThanEquals("update_dt", updateDt)))
                .update();
    }

    @Override
    public int findCountByLessThanEqualsForkDtAndGreaterThenFailPushCount(long forkDt, long failPushCount) {
        return select()
                .where(Conditions.lessThanEquals("fork_dt", forkDt))
                .where(Conditions.greaterThen("fail_push_count", failPushCount))
                .findCount();
    }

    @Override
    public int updateStatusByStatussAndLessThanEqualsForkDtAndGreaterThenFailPushCount(Collection<Short> whereStatuss, long forkDt, long failPushCount, BlockInfo.Status updateStatus) {
        return update().set("status", updateStatus.value)
                .where(Conditions.in("status", whereStatuss))
                .where(Conditions.lessThanEquals("fork_dt", forkDt))
                .where(Conditions.greaterThen("fail_push_count", failPushCount))
                .update();
    }

    @Override
    public Integer findByLimitStartRow(int startRow) {
        String sql = "select id from "+TABLE_NAME+" order by id desc limit ?,1";
        return super.getJdbcTemplate().query(sql, new Object[]{ startRow }, rs -> {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return null;
        });
    }

    @Override
    public int deleteByLessThenEqualsIdLimitDeleteRows(int id, int deleteRows) {
        return delete()
                .where(Conditions.lessThanEquals("id", id))
                .limit(deleteRows)
                .delete();
    }

    @Override
    public int findLessThanDt(long dt) {
        return delete()
                .where(Conditions.lessThanEquals("dt", dt))
                .delete();
    }

    @Override
    public Optional<BlockInfo> findByIdDesc() {
        return select().orderBy(Sort.of("id", Sort.Direction.DESC)).limit(1).findOne();
    }
}
