package com.shiyiju.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 钱包信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletVO {
    /** 可用余额 */
    private BigDecimal balance;
    /** 冻结金额 */
    private BigDecimal freezeAmount;
    /** 待结算金额 */
    private BigDecimal pendingAmount;
    /** 保证金 */
    private BigDecimal depositAmount;
    /** 累计收入 */
    private BigDecimal totalIncome;
    /** 累计提现 */
    private BigDecimal totalWithdraw;
}
