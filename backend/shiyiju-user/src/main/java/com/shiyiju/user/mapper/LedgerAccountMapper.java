package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.LedgerAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 账本账户 Mapper
 */
@Mapper
public interface LedgerAccountMapper extends BaseMapper<LedgerAccount> {

    /** 入账（余额增加） */
    @Update("UPDATE ledger_account SET balance = balance + #{amount}, version = version + 1 " +
            "WHERE user_id = #{userId} AND version = #{version}")
    int creditBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    /** 出账（余额减少，需余额校验） */
    @Update("UPDATE ledger_account SET balance = balance - #{amount}, version = version + 1 " +
            "WHERE user_id = #{userId} AND balance >= #{amount} AND version = #{version}")
    int debitBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);
}
