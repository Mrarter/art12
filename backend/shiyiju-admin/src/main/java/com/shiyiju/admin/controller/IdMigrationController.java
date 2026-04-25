package com.shiyiju.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ID迁移管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/id-migration")
@RequiredArgsConstructor
public class IdMigrationController {

    @Autowired(required = false)
    private IdMigrationService idMigrationService;

    /**
     * 执行全量ID迁移
     */
    @PostMapping("/migrate-all")
    public Map<String, Object> migrateAll() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateAllIds();
                result.put("success", true);
                result.put("message", "ID迁移任务执行成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化，请检查数据库连接");
            }
        } catch (Exception e) {
            log.error("ID迁移失败", e);
            result.put("success", false);
            result.put("message", "ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移用户ID
     */
    @PostMapping("/migrate/users")
    public Map<String, Object> migrateUsers() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateUserIds();
                result.put("success", true);
                result.put("message", "用户ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("用户ID迁移失败", e);
            result.put("success", false);
            result.put("message", "用户ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移拍卖专场ID
     */
    @PostMapping("/migrate/sessions")
    public Map<String, Object> migrateSessions() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateAuctionSessionIds();
                result.put("success", true);
                result.put("message", "专场ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("专场ID迁移失败", e);
            result.put("success", false);
            result.put("message", "专场ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移拍品ID
     */
    @PostMapping("/migrate/lots")
    public Map<String, Object> migrateLots() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateAuctionLotIds();
                result.put("success", true);
                result.put("message", "拍品ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("拍品ID迁移失败", e);
            result.put("success", false);
            result.put("message", "拍品ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移帖子ID
     */
    @PostMapping("/migrate/posts")
    public Map<String, Object> migratePosts() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migratePostIds();
                result.put("success", true);
                result.put("message", "帖子ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("帖子ID迁移失败", e);
            result.put("success", false);
            result.put("message", "帖子ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移评论ID
     */
    @PostMapping("/migrate/comments")
    public Map<String, Object> migrateComments() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateCommentIds();
                result.put("success", true);
                result.put("message", "评论ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("评论ID迁移失败", e);
            result.put("success", false);
            result.put("message", "评论ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移话题ID
     */
    @PostMapping("/migrate/topics")
    public Map<String, Object> migrateTopics() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateTopicIds();
                result.put("success", true);
                result.put("message", "话题ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("话题ID迁移失败", e);
            result.put("success", false);
            result.put("message", "话题ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移提现记录ID
     */
    @PostMapping("/migrate/withdraws")
    public Map<String, Object> migrateWithdraws() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateWithdrawIds();
                result.put("success", true);
                result.put("message", "提现记录ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("提现记录ID迁移失败", e);
            result.put("success", false);
            result.put("message", "提现记录ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移佣金记录ID
     */
    @PostMapping("/migrate/commissions")
    public Map<String, Object> migrateCommissions() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateCommissionIds();
                result.put("success", true);
                result.put("message", "佣金记录ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("佣金记录ID迁移失败", e);
            result.put("success", false);
            result.put("message", "佣金记录ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移竞拍记录ID
     */
    @PostMapping("/migrate/bids")
    public Map<String, Object> migrateBids() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateBidIds();
                result.put("success", true);
                result.put("message", "竞拍记录ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("竞拍记录ID迁移失败", e);
            result.put("success", false);
            result.put("message", "竞拍记录ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 迁移售后记录ID
     */
    @PostMapping("/migrate/aftersales")
    public Map<String, Object> migrateAftersales() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                idMigrationService.migrateRefundIds();
                result.put("success", true);
                result.put("message", "售后记录ID迁移成功");
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("售后记录ID迁移失败", e);
            result.put("success", false);
            result.put("message", "售后记录ID迁移失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取迁移状态
     */
    @GetMapping("/status")
    public Map<String, Object> getMigrationStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (idMigrationService != null) {
                Map<String, Long> status = idMigrationService.getMigrationStatus();
                result.put("success", true);
                result.put("data", status);
            } else {
                result.put("success", false);
                result.put("message", "迁移服务未初始化");
            }
        } catch (Exception e) {
            log.error("获取迁移状态失败", e);
            result.put("success", false);
            result.put("message", "获取迁移状态失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 验证ID格式
     */
    @GetMapping("/validate/{id}")
    public Map<String, Object> validateId(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean isValid = com.shiyiju.product.config.IdGenerator.isValid(id);
            result.put("success", true);
            result.put("valid", isValid);
            if (isValid) {
                result.put("type", com.shiyiju.product.config.IdGenerator.getTypeDescription(id));
                result.put("date", com.shiyiju.product.config.IdGenerator.extractDate(id));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("valid", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
