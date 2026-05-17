package com.shiyiju.user.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.user.entity.LedgerAccount;
import com.shiyiju.user.entity.LedgerTransaction;
import com.shiyiju.user.service.ledger.LedgerService;
import com.shiyiju.user.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资金审计中心（Admin）
 *
 * 功能：
 * - ledger 对账
 * - wallet_balance vs ledger sum 校验
 * - refund trace 查询
 * - 用户资金轨迹回放
 */
@Slf4j
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class FinanceAuditController {

    private final LedgerService ledgerService;
    private final WalletService walletService;

    @Value("${wallet.admin-key:shiyiju-wallet-admin-2026}")
    private String adminKey;

    /**
     * 用户资金对账
     * GET /admin/audit/reconciliation/{userId}
     *
     * 检查：ledger_balance == wallet_balance
     */
    @GetMapping("/reconciliation/{userId}")
    public Result<Map<String, Object>> reconciliation(
            @PathVariable Long userId,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");

        LedgerAccount ledgerAccount = ledgerService.getAccount(userId);
        BigDecimal walletBalance = walletService.getBalance(userId);
        BigDecimal ledgerBalance = ledgerAccount != null ? ledgerAccount.getBalance() : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("ledgerBalance", ledgerBalance);
        result.put("walletBalance", walletBalance);
        result.put("isConsistent", ledgerBalance.compareTo(walletBalance) == 0);

        return Result.success(result);
    }

    /**
     * 退款链路追踪
     * GET /admin/audit/refund-trace/{orderNo}
     */
    @GetMapping("/refund-trace/{orderNo}")
    public Result<List<LedgerTransaction>> refundTrace(
            @PathVariable String orderNo,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");

        // 查找该订单的所有交易
        List<LedgerTransaction> txns = Collections.emptyList();
        // 生产环境应通过 relatedId + relatedType 查询
        return Result.success(txns);
    }

    /**
     * 用户资金轨迹回放
     * GET /admin/audit/transaction-history/{userId}
     */
    @GetMapping("/transaction-history/{userId}")
    public Result<List<LedgerTransaction>> transactionHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        return Result.success(ledgerService.listTransactions(userId, page, pageSize));
    }

    /**
     * 全量对账报告
     * GET /admin/audit/full-reconciliation
     */
    @GetMapping("/full-reconciliation")
    public Result<Map<String, Object>> fullReconciliation(
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        // 返回对账SQL模板
        Map<String, Object> result = new HashMap<>();
        result.put("reconciliationSql",
                "SELECT u.id AS user_id,\n" +
                "       COALESCE(l.balance, 0) AS ledger_balance,\n" +
                "       COALESCE(w.balance, 0) AS wallet_balance,\n" +
                "       CASE WHEN COALESCE(l.balance, 0) = COALESCE(w.balance, 0)\n" +
                "            THEN 'OK' ELSE 'MISMATCH' END AS status\n" +
                "FROM user_account u\n" +
                "LEFT JOIN ledger_account l ON l.user_id = u.id\n" +
                "LEFT JOIN user_wallet w ON w.user_id = u.id\n" +
                "WHERE COALESCE(l.balance, 0) != COALESCE(w.balance, 0);");
        result.put("note", "执行此SQL可找出所有余额不一致的用户");
        return Result.success(result);
    }
}
