package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.LedgerTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 账本交易 Mapper
 */
@Mapper
public interface LedgerTransactionMapper extends BaseMapper<LedgerTransaction> {

    /** 按 txn_id 查找（幂等） */
    @Select("SELECT * FROM ledger_transaction WHERE txn_id = #{txnId} LIMIT 1")
    LedgerTransaction findByTxnId(@Param("txnId") String txnId);

    /** 查找原始交易的反向交易 */
    @Select("SELECT * FROM ledger_transaction WHERE reversal_of_txn_id = #{txnId} LIMIT 1")
    LedgerTransaction findReversalByTxnId(@Param("txnId") String txnId);
}
