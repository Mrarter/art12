package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 社区管理控制器
 */
@RestController
@RequestMapping("/admin/community")
public class CommunityAdminController {

    private static final Logger log = LoggerFactory.getLogger(CommunityAdminController.class);

    /**
     * 帖子列表
     */
    @GetMapping("/posts")
    public Result<PageResult<Map<String, Object>>> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        List<Map<String, Object>> posts = new ArrayList<>();
        String[] contents = {"分享一幅作品", "今天去了画展", "新人报道"};
        String[] nicknames = {"画家小王", "艺术爱好者", "新手求带"};
        int[] statuses = {1, 2, 3};
        for (int i = 0; i < contents.length; i++) {
            if (status != null && statuses[i] != status) continue;
            Map<String, Object> post = new HashMap<>();
            post.put("id", i + 1);
            post.put("nickname", nicknames[i]);
            post.put("content", contents[i]);
            post.put("likeCount", 10 + i * 5);
            post.put("commentCount", 2 + i);
            post.put("status", statuses[i]);
            post.put("statusText", statuses[i] == 1 ? "正常" : statuses[i] == 2 ? "审核中" : "违规");
            post.put("createTime", "2024-04-10 10:30:00");
            posts.add(post);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(posts);
        result.setTotal((long) posts.size());
        return Result.success(result);
    }

    /**
     * 审核帖子通过
     */
    @PostMapping("/posts/{id}/approve")
    public Result<Void> approvePost(@PathVariable Long id) {
        log.info("审核帖子通过: id={}", id);
        return Result.success();
    }

    /**
     * 审核帖子拒绝
     */
    @PostMapping("/posts/{id}/reject")
    public Result<Void> rejectPost(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        log.info("审核帖子拒绝: id={}, reason={}", id, params.get("reason"));
        return Result.success();
    }

    /**
     * 评论列表
     */
    @GetMapping("/comments")
    public Result<PageResult<Map<String, Object>>> getComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> comments = new ArrayList<>();
        String[] contents = {"画得真棒！", "学习了", "支持一下"};
        for (int i = 0; i < contents.length; i++) {
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", i + 1);
            comment.put("nickname", "用户" + (i + 1));
            comment.put("content", contents[i]);
            comment.put("createTime", "2024-04-10 10:30:00");
            comments.add(comment);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(comments);
        result.setTotal(3L);
        return Result.success(result);
    }

    /**
     * 话题列表
     */
    @GetMapping("/topics")
    public Result<PageResult<Map<String, Object>>> getTopics(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> topics = new ArrayList<>();
        String[] names = {"每日分享", "艺术创作", "收藏心得", "拍卖预告"};
        int[] postCounts = {1250, 890, 560, 320};
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> topic = new HashMap<>();
            topic.put("id", i + 1);
            topic.put("name", names[i]);
            topic.put("postCount", postCounts[i]);
            topic.put("followCount", 100 + i * 50);
            topics.add(topic);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(topics);
        result.setTotal(4L);
        return Result.success(result);
    }

    /**
     * 创建话题
     */
    @PostMapping("/topics")
    public Result<Void> createTopic(@RequestBody Map<String, Object> params) {
        log.info("创建话题: {}", params);
        return Result.success();
    }

    /**
     * 举报列表
     */
    @GetMapping("/reports")
    public Result<PageResult<Map<String, Object>>> getReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> reports = new ArrayList<>();
        String[] reportTypes = {"涉黄内容", "垃圾广告", "人身攻击"};
        String[] contents = {"帖子内容含不良信息", "推广二维码", "恶意评论"};
        for (int i = 0; i < reportTypes.length; i++) {
            Map<String, Object> report = new HashMap<>();
            report.put("id", i + 1);
            report.put("reporterNickname", "举报人" + (i + 1));
            report.put("reportedNickname", "被举报人" + (i + 1));
            report.put("reportType", reportTypes[i]);
            report.put("content", contents[i]);
            report.put("status", i == 0 ? 0 : 1);
            report.put("statusText", i == 0 ? "待处理" : "已处理");
            report.put("createTime", "2024-04-15 10:30:00");
            reports.add(report);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(reports);
        result.setTotal(3L);
        return Result.success(result);
    }

    /**
     * 处理举报
     */
    @PostMapping("/reports/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        log.info("处理举报: id={}, action={}", id, params.get("action"));
        return Result.success();
    }
}
