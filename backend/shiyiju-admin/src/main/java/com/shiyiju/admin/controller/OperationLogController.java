package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.OperationLogService;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 操作日志控制器（真实持久化版本）
 */
@RestController
@RequestMapping("/admin/operation-log")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 操作日志列表（真实查询数据库）
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getLogList(
            @RequestParam(required = false) String adminName,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<Map<String, Object>> result = operationLogService.getLogs(adminName, operation, startDate, endDate, page, size);
        return Result.success(result);
    }

    /**
     * 操作日志统计（真实查询）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(operationLogService.getStats());
    }
}
