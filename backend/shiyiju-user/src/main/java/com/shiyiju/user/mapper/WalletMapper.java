package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 钱包 Mapper
 */
@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {

    @Insert("INSERT IGNORE INTO user_wallet " +
            "(user_id, balance, freeze_amount, pending_amount, deposit_amount, total_income, total_withdraw, version, created_time, updated_time) " +
            "VALUES (#{userId}, 0, 0, 0, 0, 0, 0, 0, NOW(), NOW())")
    int ensureWallet(@Param("userId") Long userId);

    /**
     * 入账（增加余额 + 累计收入）
     */
    @Update("UPDATE user_wallet SET balance = balance + #{amount}, total_income = total_income + #{amount}, version = version + 1 " +
            "WHERE user_id = #{userId}")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    /**
     * 冻结入账（外部收入 → 冻结金额 + 累计收入）
     */
    @Update("UPDATE user_wallet SET freeze_amount = freeze_amount + #{amount}, total_income = total_income + #{amount}, version = version + 1 " +
            "WHERE user_id = #{userId}")
    int addFrozenIncome(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    /**
     * 出账（扣减余额 + 累计提现）
     */
    @Update("UPDATE user_wallet SET balance = balance - #{amount}, total_withdraw = total_withdraw + #{amount}, version = version + 1 " +
            "WHERE user_id = #{userId} AND balance >= #{amount} AND version = #{version}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    /**
     * 冻结金额（余额 → 冻结）
     */
    @Update("UPDATE user_wallet SET balance = balance - #{amount}, freeze_amount = freeze_amount + #{amount}, version = version + 1 " +
            "WHERE user_id = #{userId} AND balance >= #{amount} AND version = #{version}")
    int freezeBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    /**
     * 解冻金额（冻结 → 余额）
     */
    @Update("UPDATE user_wallet SET balance = balance + #{amount}, freeze_amount = freeze_amount - #{amount}, version = version + 1 " +
            "WHERE user_id = #{userId} AND freeze_amount >= #{amount} AND version = #{version}")
    int unfreezeBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    /**
     * 销售款解冻（冻结金额 → 可用余额）
     */
    @Update("UPDATE user_wallet SET balance = balance + #{amount}, freeze_amount = freeze_amount - #{amount}, version = version + 1 " +
            "WHERE user_id = #{userId} AND freeze_amount >= #{amount} AND version = #{version}")
    int releaseFrozenIncome(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);
}
