package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 操作日志控制器
 */
@RestController
@RequestMapping("/admin/operation-log")
public class OperationLogController {

    /**
     * 操作日志列表
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getLogList(
            @RequestParam(required = false) String adminName,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        String[] operations = {"登录", "创建作品", "审核通过", "修改配置", "删除订单"};
        String[] modules = {"系统", "作品", "用户", "配置", "订单"};
        
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("id", i);
            log.put("adminName", "admin");
            log.put("module", modules[i % 5]);
            log.put("operation", operations[i % 5]);
            log.put("detail", "操作详情" + i);
            log.put("ip", "192.168.1." + (100 + i));
            log.put("createTime", "2024-04-" + String.format("%02d", 15 - i % 10) + " " + String.format("%02d", 10 + i % 8) + ":30:00");
            list.add(log);
        }
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(100L);
        result.setPage(page);
        result.setPageSize(size);
        return Result.success(result);
    }

    /**
     * 操作日志统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayCount", 25);
        stats.put("weekCount", 156);
        stats.put("monthCount", 680);
        stats.put("topOperations", Arrays.asList(
            Map.of("operation", "登录", "count", 120),
            Map.of("operation", "修改配置", "count", 85),
            Map.of("operation", "审核操作", "count", 60)
        ));
        return Result.success(stats);
    }
}