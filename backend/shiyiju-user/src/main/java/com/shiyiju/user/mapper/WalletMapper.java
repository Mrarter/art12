package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 钱包 Mapper
 */
@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {

    /**
     * 入账（增加余额 + 累计收入）
     */
    @Update("UPDATE user_wallet SET balance = balance + #{amount}, total_income = total_income + #{amount}, version = version + 1 " +
            "WHERE user_id = #{userId} AND version = #{version}")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

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
}
