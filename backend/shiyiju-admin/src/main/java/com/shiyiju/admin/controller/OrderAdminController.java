package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 订单管理控制器
 */
@RestController
@RequestMapping("/admin/order")
public class OrderAdminController {

    /**
     * 订单列表
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
        
        List<Map<String, Object>> list = new ArrayList<>();
        String[] statuses = {"已完成", "待支付", "已取消", "退款中"};
        String[] payStatuses = {"已支付", "未支付", "已退款"};
        
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> order = new LinkedHashMap<>();
            order.put("orderId", "ORD" + String.format("%010d", 10000 + i));
            order.put("userName", "用户" + i);
            order.put("userPhone", "138****" + String.format("%04d", 1000 + i));
            order.put("artworkTitle", "作品标题" + i);
            order.put("cover", "http://example.com/image.jpg");
            order.put("amount", 5000.00 + i * 1000);
            order.put("status", i % 4);
            order.put("statusText", statuses[i % 4]);
            order.put("payStatus", i % 2 == 0 ? "已支付" : "未支付");
            order.put("createTime", "2024-04-" + String.format("%02d", 10 + i) + " " + String.format("%02d", 10 + i) + ":30:00");
            order.put("payTime", i % 2 == 0 ? "2024-04-" + String.format("%02d", 10 + i) + " " + String.format("%02d", 11 + i) + ":00:00" : null);
            list.add(order);
        }
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(50L);
        result.setPage(pageNum);
        result.setPageSize(pageSize);
        return Result.success(result);
    }

    /**
     * 售后/退款列表
     */
    @GetMapping("/aftersale/list")
    public Result<PageResult<Map<String, Object>>> getAftersaleList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        String[] statuses = {"处理中", "已完成", "已拒绝"};
        
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> aftersale = new LinkedHashMap<>();
            aftersale.put("id", i);
            aftersale.put("orderNo", "ORD" + String.format("%010d", 10000 + i));
            aftersale.put("userName", "用户" + i);
            aftersale.put("artworkTitle", "作品标题" + i);
            aftersale.put("applyAmount", 1000.00 + i * 500);
            aftersale.put("reason", "不喜欢 / 描述不符");
            aftersale.put("status", i - 1);
            aftersale.put("statusText", statuses[i % 3]);
            aftersale.put("applyTime", "2024-04-" + String.format("%02d", 15 + i) + " 10:30:00");
            aftersale.put("handleTime", i > 1 ? "2024-04-" + String.format("%02d", 16 + i) + " 10:30:00" : null);
            list.add(aftersale);
        }
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(10L);
        result.setPage(page);
        result.setPageSize(size);
        return Result.success(result);
    }

    /**
     * 订单详情
     */
    @GetMapping("/detail/{orderId}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable String orderId) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId", orderId);
        order.put("userName", "用户1");
        order.put("userPhone", "13800138001");
        order.put("artworkTitle", "作品标题");
        order.put("amount", 5000.00);
        order.put("status", 2);
        order.put("createTime", "2024-04-15 10:30:00");
        return Result.success(order);
    }

    /**
     * 处理售后
     */
    @PostMapping("/aftersale/handle")
    public Result<Void> handleAftersale(@RequestBody Map<String, Object> params) {
        return Result.success();
    }
}