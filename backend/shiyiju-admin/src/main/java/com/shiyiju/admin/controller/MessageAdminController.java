package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 消息管理控制器
 */
@RestController
@RequestMapping("/admin/message")
public class MessageAdminController {

    /**
     * 消息列表
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getMessageList(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        String[] titles = {"系统通知", "活动提醒", "订单更新"};
        
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("id", i);
            msg.put("title", titles[i % 3]);
            msg.put("content", "消息内容" + i);
            msg.put("type", i % 3);
            msg.put("targetType", "ALL");
            msg.put("sendCount", 1000 + i * 100);
            msg.put("readCount", 500 + i * 50);
            msg.put("createTime", "2024-04-" + String.format("%02d", 15 + i) + " 10:30:00");
            list.add(msg);
        }
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(20L);
        result.setPage(page);
        result.setPageSize(size);
        return Result.success(result);
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<Void> send(@RequestBody Map<String, Object> params) {
        return Result.success();
    }

    /**
     * 消息模板列表
     */
    @GetMapping("/template/list")
    public Result<List<Map<String, Object>>> getTemplateList() {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] names = {"注册欢迎", "订单通知", "拍卖提醒", "活动通知"};
        
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> template = new LinkedHashMap<>();
            template.put("id", i + 1);
            template.put("name", names[i]);
            template.put("content", "模板内容" + (i + 1));
            template.put("variables", "username,time");
            template.put("status", i < 2 ? "ENABLED" : "DISABLED");
            list.add(template);
        }
        return Result.success(list);
    }

    /**
     * 创建消息模板
     */
    @PostMapping("/template")
    public Result<Long> createTemplate(@RequestBody Map<String, Object> params) {
        return Result.success(System.currentTimeMillis());
    }

    /**
     * 更新消息模板
     */
    @PutMapping("/template/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return Result.success();
    }

    /**
     * 删除消息模板
     */
    @DeleteMapping("/template/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        return Result.success();
    }

    /**
     * 更新消息模板状态
     */
    @PutMapping("/template/{id}/status")
    public Result<Void> updateTemplateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return Result.success();
    }

    /**
     * 消息统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSent", 50000);
        stats.put("todaySent", 120);
        stats.put("totalRead", 35000);
        stats.put("readRate", 70.0);
        return Result.success(stats);
    }
}