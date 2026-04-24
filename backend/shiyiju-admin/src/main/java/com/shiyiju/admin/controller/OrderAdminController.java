package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.OrderService;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 订单管理控制器（真实持久化版本）
 */
@RestController
@RequestMapping("/admin/order")
public class OrderAdminController {

    private static final Logger log = LoggerFactory.getLogger(OrderAdminController.class);

    private final OrderService orderService;

    public OrderAdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 订单列表（真实查询）
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getOrderList(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        PageResult<Map<String, Object>> result = orderService.getOrders(orderNo, userName, status, startDate, endDate, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 订单详情
     */
    @GetMapping("/detail/{orderId}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long orderId) {
        Map<String, Object> order = orderService.getOrderById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 确认发货
     */
    @PostMapping("/ship")
    public Result<Void> shipOrder(@RequestBody Map<String, Object> params) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        String expressCompany = params.get("expressCompany") != null ? params.get("expressCompany").toString() : null;
        String expressNo = params.get("expressNo") != null ? params.get("expressNo").toString() : null;

        try {
            orderService.shipOrder(orderId, expressCompany, expressNo);
            log.info("确认发货成功: orderId={}", orderId);
            return Result.success();
        } catch (Exception e) {
            log.error("确认发货失败", e);
            return Result.fail("操作失败: " + e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public Result<Void> cancelOrder(@RequestBody Map<String, Object> params) {
        Long orderId = Long.parseLong(params.get("orderId").toString());
        String reason = params.get("reason") != null ? params.get("reason").toString() : "";

        try {
            orderService.cancelOrder(orderId, reason);
            log.info("取消订单成功: orderId={}", orderId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.fail("操作失败: " + e.getMessage());
        }
    }

    /**
     * 售后/退款列表（真实查询）
     */
    @GetMapping("/aftersale/list")
    public Result<PageResult<Map<String, Object>>> getAftersaleList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResult<Map<String, Object>> result = orderService.getRefunds(status, page, size);
        return Result.success(result);
    }

    /**
     * 处理售后
     */
    @PostMapping("/aftersale/handle")
    public Result<Void> handleAftersale(@RequestBody Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        Integer status = Integer.parseInt(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : "";

        try {
            orderService.handleRefund(id, status, remark);
            log.info("处理售后成功: id={}, status={}", id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("处理售后失败", e);
            return Result.fail("操作失败: " + e.getMessage());
        }
    }
}
