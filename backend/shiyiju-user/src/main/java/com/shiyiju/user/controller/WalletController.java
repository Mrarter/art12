package com.shiyiju.user.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.user.entity.Wallet;
import com.shiyiju.user.entity.WalletBill;
import com.shiyiju.user.mapper.WalletBillMapper;
import com.shiyiju.user.service.WalletService;
import com.shiyiju.user.vo.WalletVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final JdbcTemplate jdbcTemplate;

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
    public Result<List<Map<String, Object>>> getBills(
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
        return Result.success(enrichBills(result.getRecords()));
    }

    private List<Map<String, Object>> enrichBills(List<WalletBill> bills) {
        Map<Long, Map<String, Object>> orderMap = loadRelatedOrders(bills);
        List<Map<String, Object>> records = new ArrayList<>();
        for (WalletBill bill : bills) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", bill.getId());
            item.put("userId", bill.getUserId());
            item.put("billType", bill.getBillType());
            item.put("amount", bill.getAmount());
            item.put("beforeBalance", bill.getBeforeBalance());
            item.put("afterBalance", bill.getAfterBalance());
            item.put("relatedId", bill.getRelatedId());
            item.put("relatedType", bill.getRelatedType());
            item.put("remark", bill.getRemark());
            item.put("createdTime", bill.getCreatedTime());

            Map<String, Object> order = orderMap.get(bill.getRelatedId());
            Object fallbackTime = order != null
                    ? firstNonNull(order.get("paid_at"), order.get("created_at"))
                    : null;
            item.put("billTime", normalizeDateTime(firstNonNull(bill.getCreatedTime(), fallbackTime)));
            if (order != null) {
                item.put("relatedNo", order.get("order_no"));
                item.put("orderNo", order.get("order_no"));
            }
            records.add(item);
        }
        return records;
    }

    private Map<Long, Map<String, Object>> loadRelatedOrders(List<WalletBill> bills) {
        List<Long> orderIds = bills.stream()
                .filter(item -> item.getRelatedId() != null)
                .filter(item -> "order".equalsIgnoreCase(String.valueOf(item.getRelatedType())))
                .map(WalletBill::getRelatedId)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = orderIds.stream().map(id -> "?").collect(Collectors.joining(","));
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, order_no, paid_at, created_at FROM trade_order WHERE id IN (" + placeholders + ")",
                    orderIds.toArray()
            );
            return rows.stream()
                    .filter(row -> row.get("id") != null)
                    .collect(Collectors.toMap(
                            row -> ((Number) row.get("id")).longValue(),
                            Function.identity(),
                            (left, right) -> left
                    ));
        } catch (Exception e) {
            log.warn("加载钱包流水关联订单失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String normalizeDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime time) {
            return time.toString();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        return Objects.toString(value, null);
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

    /** 冻结入账（服务调用） */
    @PostMapping("/admin/frozen-income")
    public Result<Void> adminFrozenIncome(@RequestBody Map<String, Object> params,
                                          @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Long userId = ((Number) params.get("userId")).longValue();
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        String billType = (String) params.getOrDefault("billType", "income_frozen");
        Long relatedId = params.get("relatedId") != null ? ((Number) params.get("relatedId")).longValue() : null;
        String relatedType = (String) params.getOrDefault("relatedType", "");
        String remark = (String) params.getOrDefault("remark", "");
        walletService.frozenIncome(userId, amount, billType, relatedId, relatedType, remark);
        return Result.success();
    }

    /** 销售款解冻（服务调用） */
    @PostMapping("/admin/release-frozen-income")
    public Result<Void> adminReleaseFrozenIncome(@RequestBody Map<String, Object> params,
                                                 @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Long userId = ((Number) params.get("userId")).longValue();
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        String billType = (String) params.getOrDefault("billType", "income_release");
        Long relatedId = params.get("relatedId") != null ? ((Number) params.get("relatedId")).longValue() : null;
        String relatedType = (String) params.getOrDefault("relatedType", "");
        String remark = (String) params.getOrDefault("remark", "");
        walletService.releaseFrozenIncome(userId, amount, billType, relatedId, relatedType, remark);
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
