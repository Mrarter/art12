package com.shiyiju.order.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.order.entity.Order;
import com.shiyiju.order.entity.OrderFailRecord;
import com.shiyiju.order.service.OrderFailRecorder;
import com.shiyiju.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单失败处理与重试控制器
 * 提供失败记录查询、手动重试接口
 */
@Slf4j
@RestController
@RequestMapping("/order/fail")
@RequiredArgsConstructor
public class OrderFailController {

    private final OrderFailRecorder orderFailRecorder;
    private final OrderService orderService;

    /**
     * 查询用户的失败记录列表
     * GET /order/fail/records
     */
    @GetMapping("/records")
    public Result<List<OrderFailRecord>> getFailRecords(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return Result.success(orderFailRecorder.getRetryableRecords(userId));
    }

    /**
     * 查询单条失败记录详情
     * GET /order/fail/records/{id}
     */
    @GetMapping("/records/{id}")
    public Result<OrderFailRecord> getFailRecordDetail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        OrderFailRecord record = orderFailRecorder.getById(id);
        if (record == null) {
            return Result.fail(ResultCode.NOT_FOUND, "失败记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            return Result.fail(ResultCode.FORBIDDEN);
        }
        return Result.success(record);
    }

    /**
     * 重试失败的订单创建
     * POST /order/fail/retry/{id}
     *
     * 前提条件：
     * 1. 失败记录存在且属于当前用户
     * 2. 重试次数未达上限
     * 3. 原失败原因可重试
     */
    @PostMapping("/retry/{id}")
    public Result<Map<String, Object>> retryOrder(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }

        OrderFailRecord record = orderFailRecorder.getById(id);
        if (record == null) {
            return Result.fail(ResultCode.ORDER_RETRY_INVALID.getCode(), "失败记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            return Result.fail(ResultCode.FORBIDDEN);
        }
        if (!orderFailRecorder.isRetryable(record)) {
            return Result.fail(ResultCode.ORDER_RETRY_EXCEEDED);
        }

        // 标记重试中
        int currentRetry = record.getRetryCount() + 1;
        orderFailRecorder.updateRetryStatus(id, currentRetry, 1);

        try {
            // 调用订单服务重试逻辑
            Order order = orderService.retryCreateOrder(record, currentRetry);

            // 重试成功：更新失败记录状态
            orderFailRecorder.updateRetryStatus(id, currentRetry, 2);

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("orderNo", order.getOrderNo());
            result.put("payAmount", order.getPayAmount());
            return Result.success("订单重试成功", result);

        } catch (Exception e) {
            log.error("订单重试失败 - recordId={}, retryCount={}, error={}", id, currentRetry, e.getMessage());

            boolean exceeded = currentRetry >= record.getMaxRetries();
            orderFailRecorder.updateRetryStatus(id, currentRetry, exceeded ? 3 : 0);

            if (exceeded) {
                return Result.fail(ResultCode.ORDER_RETRY_EXCEEDED);
            }
            return Result.fail(ResultCode.ORDER_CREATE_FAILED.getCode(),
                    "重试失败 (" + currentRetry + "/" + record.getMaxRetries() + "): " + e.getMessage());
        }
    }
}
