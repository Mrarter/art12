package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.MessageAdminPersistenceService;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员 - 消息管理控制器
 */
@RestController
@RequestMapping("/admin/message")
public class MessageAdminController {

    private final MessageAdminPersistenceService messageAdminPersistenceService;

    public MessageAdminController(MessageAdminPersistenceService messageAdminPersistenceService) {
        this.messageAdminPersistenceService = messageAdminPersistenceService;
    }

    /**
     * 消息列表
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getMessageList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(messageAdminPersistenceService.listMessages(type, status, page, size));
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<Long> send(@RequestBody Map<String, Object> params) {
        return Result.success(messageAdminPersistenceService.sendMessage(params));
    }

    /**
     * 消息模板列表
     */
    @GetMapping("/template/list")
    public Result<List<Map<String, Object>>> getTemplateList() {
        return Result.success(messageAdminPersistenceService.listTemplates());
    }

    /**
     * 创建消息模板
     */
    @PostMapping("/template")
    public Result<Long> createTemplate(@RequestBody Map<String, Object> params) {
        return Result.success(messageAdminPersistenceService.createTemplate(params));
    }

    /**
     * 更新消息模板
     */
    @PutMapping("/template/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        messageAdminPersistenceService.updateTemplate(id, params);
        return Result.success();
    }

    /**
     * 删除消息模板
     */
    @DeleteMapping("/template/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        messageAdminPersistenceService.deleteTemplate(id);
        return Result.success();
    }

    /**
     * 更新消息模板状态
     */
    @PutMapping("/template/{id}/status")
    public Result<Void> updateTemplateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        int status = params.get("status") instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(params.get("status")));
        messageAdminPersistenceService.updateTemplateStatus(id, status);
        return Result.success();
    }

    /**
     * 消息统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(messageAdminPersistenceService.getStats());
    }
}
