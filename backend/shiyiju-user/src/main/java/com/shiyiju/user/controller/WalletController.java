package com.shiyiju.user.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.user.entity.Wallet;
import com.shiyiju.user.entity.WalletBill;
import com.shiyiju.user.mapper.WalletBillMapper;
import com.shiyiju.user.service.WalletService;
import com.shiyiju.user.vo.WalletVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 钱包控制器
 */
@Slf4j
@RestController
@RequestMapping("/user/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletBillMapper walletBillMapper;

    @Value("${wallet.admin-key:shiyiju-wallet-admin-2026}")
    private String adminKey;

    // ===================== 用户端点 =====================

    @GetMapping("/info")
    public Result<WalletVO> getWallet(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return Result.fail(401, "请先登录");
        Wallet wallet = walletService.getWallet(userId);
        WalletVO vo = WalletVO.builder()
                .balance(wallet.getBalance())
                .freezeAmount(wallet.getFreezeAmount())
                .pendingAmount(wallet.getPendingAmount())
                .depositAmount(wallet.getDepositAmount())
                .totalIncome(wallet.getTotalIncome())
                .totalWithdraw(wallet.getTotalWithdraw())
                .build();
        return Result.success(vo);
    }

    @GetMapping("/bills")
    public Result<List<WalletBill>> getBills(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        if (userId == null) return Result.fail(401, "请先登录");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WalletBill> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WalletBill> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WalletBill>()
                        .eq(WalletBill::getUserId, userId)
                        .orderByDesc(WalletBill::getCreatedTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WalletBill> result =
                walletBillMapper.selectPage(p, wrapper);
        return Result.success(result.getRecords());
    }

    // ===================== 服务间调用端点 =====================

    /** 入账（服务调用） */
    @PostMapping("/admin/income")
    public Result<Void> adminIncome(@RequestBody Map<String, Object> params,
                                     @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Long userId = ((Number) params.get("userId")).longValue();
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        String billType = (String) params.getOrDefault("billType", "income");
        Long relatedId = params.get("relatedId") != null ? ((Number) params.get("relatedId")).longValue() : null;
        String relatedType = (String) params.getOrDefault("relatedType", "");
        String remark = (String) params.getOrDefault("remark", "");
        walletService.income(userId, amount, billType, relatedId, relatedType, remark);
        return Result.success();
    }

    /** 出账（服务调用） */
    @PostMapping("/admin/expense")
    public Result<Void> adminExpense(@RequestBody Map<String, Object> params,
                                      @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Long userId = ((Number) params.get("userId")).longValue();
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        String billType = (String) params.getOrDefault("billType", "withdraw");
        Long relatedId = params.get("relatedId") != null ? ((Number) params.get("relatedId")).longValue() : null;
        String relatedType = (String) params.getOrDefault("relatedType", "");
        String remark = (String) params.getOrDefault("remark", "");
        walletService.expense(userId, amount, billType, relatedId, relatedType, remark);
        return Result.success();
    }

    /** 冻结（服务调用） */
    @PostMapping("/admin/freeze")
    public Result<Void> adminFreeze(@RequestBody Map<String, Object> params,
                                     @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Long userId = ((Number) params.get("userId")).longValue();
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        Long relatedId = params.get("relatedId") != null ? ((Number) params.get("relatedId")).longValue() : null;
        String relatedType = (String) params.getOrDefault("relatedType", "");
        String remark = (String) params.getOrDefault("remark", "");
        walletService.freeze(userId, amount, relatedId, relatedType, remark);
        return Result.success();
    }

    /** 解冻（服务调用） */
    @PostMapping("/admin/unfreeze")
    public Result<Void> adminUnfreeze(@RequestBody Map<String, Object> params,
                                       @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Long userId = ((Number) params.get("userId")).longValue();
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        Long relatedId = params.get("relatedId") != null ? ((Number) params.get("relatedId")).longValue() : null;
        String relatedType = (String) params.getOrDefault("relatedType", "");
        String remark = (String) params.getOrDefault("remark", "");
        walletService.unfreeze(userId, amount, relatedId, relatedType, remark);
        return Result.success();
    }
}
