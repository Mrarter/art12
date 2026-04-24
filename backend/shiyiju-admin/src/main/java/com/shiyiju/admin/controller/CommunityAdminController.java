package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.CommunityService;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 社区管理控制器（真实持久化版本）
 */
@RestController
@RequestMapping("/admin/community")
public class CommunityAdminController {

    private static final Logger log = LoggerFactory.getLogger(CommunityAdminController.class);

    @Autowired
    private CommunityService communityService;

    // ==================== 帖子管理 ====================

    /**
     * 帖子列表（真实查询）
     */
    @GetMapping("/posts")
    public Result<PageResult<Map<String, Object>>> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {

        PageResult<Map<String, Object>> pageResult = communityService.getPosts(page, size, status);
        return Result.success(pageResult);
    }

    /**
     * 审核帖子通过（真保存）
     */
    @PostMapping("/posts/{id}/approve")
    public Result<Void> approvePost(@PathVariable Long id) {
        try {
            communityService.approvePost(id);
            log.info("审核帖子通过: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("审核帖子失败", e);
            return Result.fail("审核失败: " + e.getMessage());
        }
    }

    /**
     * 审核帖子拒绝（真保存）
     */
    @PostMapping("/posts/{id}/reject")
    public Result<Void> rejectPost(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            String reason = params.get("reason") != null ? params.get("reason").toString() : "";
            communityService.rejectPost(id, reason);
            log.info("审核帖子拒绝: id={}, reason={}", id, reason);
            return Result.success();
        } catch (Exception e) {
            log.error("审核帖子失败", e);
            return Result.fail("审核失败: " + e.getMessage());
        }
    }

    /**
     * 删除帖子（真删除）
     */
    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        try {
            communityService.deletePost(id);
            log.info("删除帖子: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除帖子失败", e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    // ==================== 评论管理 ====================

    /**
     * 评论列表（真实查询）
     */
    @GetMapping("/comments")
    public Result<PageResult<Map<String, Object>>> getComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Integer status) {

        PageResult<Map<String, Object>> pageResult = communityService.getComments(page, size, postId, status);
        return Result.success(pageResult);
    }

    /**
     * 审核评论通过（真保存）
     */
    @PostMapping("/comments/{id}/approve")
    public Result<Void> approveComment(@PathVariable Long id) {
        try {
            communityService.approveComment(id);
            log.info("审核评论通过: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("审核评论失败", e);
            return Result.fail("审核失败: " + e.getMessage());
        }
    }

    /**
     * 审核评论拒绝（真保存）
     */
    @PostMapping("/comments/{id}/reject")
    public Result<Void> rejectComment(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            String reason = params.get("reason") != null ? params.get("reason").toString() : "";
            communityService.rejectComment(id, reason);
            log.info("审核评论拒绝: id={}, reason={}", id, reason);
            return Result.success();
        } catch (Exception e) {
            log.error("审核评论失败", e);
            return Result.fail("审核失败: " + e.getMessage());
        }
    }

    /**
     * 删除评论（真删除）
     */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        try {
            communityService.deleteComment(id);
            log.info("删除评论: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除评论失败", e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    // ==================== 话题管理 ====================

    /**
     * 话题列表（真实查询）
     */
    @GetMapping("/topics")
    public Result<List<Map<String, Object>>> getTopics() {
        return Result.success(communityService.getTopics());
    }

    /**
     * 创建话题（真保存）
     */
    @PostMapping("/topics")
    public Result<Long> createTopic(@RequestBody Map<String, Object> params) {
        try {
            String name = params.get("name").toString();
            String icon = params.get("icon") != null ? params.get("icon").toString() : "";
            Integer sort = params.get("sort") != null ? Integer.parseInt(params.get("sort").toString()) : 0;
            Long id = communityService.createTopic(name, icon, sort);
            log.info("创建话题成功: {}", name);
            return Result.success(id);
        } catch (Exception e) {
            log.error("创建话题失败", e);
            return Result.fail("创建话题失败: " + e.getMessage());
        }
    }

    /**
     * 更新话题（真保存）
     */
    @PutMapping("/topics/{id}")
    public Result<Void> updateTopic(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            communityService.updateTopic(id, params);
            log.info("更新话题成功: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("更新话题失败", e);
            return Result.fail("更新话题失败: " + e.getMessage());
        }
    }

    /**
     * 删除话题
     */
    @DeleteMapping("/topics/{id}")
    public Result<Void> deleteTopic(@PathVariable Long id) {
        try {
            communityService.deleteTopic(id);
            log.info("删除话题: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除话题失败", e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    // ==================== 举报管理 ====================

    /**
     * 举报列表（真实查询）
     */
    @GetMapping("/reports")
    public Result<PageResult<Map<String, Object>>> getReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {

        PageResult<Map<String, Object>> pageResult = communityService.getReports(page, size, status);
        return Result.success(pageResult);
    }

    /**
     * 处理举报（真保存）
     */
    @PostMapping("/reports/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            String result = params.get("result") != null ? params.get("result").toString() : "";
            communityService.handleReport(id, result);
            log.info("处理举报成功: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("处理举报失败", e);
            return Result.fail("处理失败: " + e.getMessage());
        }
    }

    // ==================== 统计 ====================

    /**
     * 社区统计（真实查询）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(communityService.getStats());
    }
}
