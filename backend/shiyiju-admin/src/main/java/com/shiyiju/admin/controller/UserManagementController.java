package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.UserAdminPersistenceService;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 管理员 - 用户、艺术家、艺荐官管理控制器
 */
@RestController
@RequestMapping("/admin/user")
public class UserManagementController {

    private final UserAdminPersistenceService userAdminPersistenceService;

    public UserManagementController(UserAdminPersistenceService userAdminPersistenceService) {
        this.userAdminPersistenceService = userAdminPersistenceService;
    }

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getUserList(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String nickname,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String identity,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(required = false) String keyword
    ) {
        String effectiveNickname = nickname;
        if ((effectiveNickname == null || effectiveNickname.isBlank()) && keyword != null && !keyword.isBlank()) {
            effectiveNickname = keyword;
        }
        return Result.success(userAdminPersistenceService.listUsers(
            page, size, userId, effectiveNickname, phone, identity, startDate, endDate
        ));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats() {
        return Result.success(userAdminPersistenceService.getUserStats());
    }

    @GetMapping("/{userId}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long userId) {
        Map<String, Object> detail = userAdminPersistenceService.getUserDetail(userId);
        if (detail == null) {
            return Result.fail(404, "用户不存在");
        }
        return Result.success(detail);
    }

    @PutMapping("/{userId}")
    public Result<Void> updateUser(@PathVariable Long userId, @RequestBody Map<String, Object> params) {
        userAdminPersistenceService.updateUser(userId, params);
        return Result.success();
    }

    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@RequestBody Map<String, Object> params) {
        Long userId = ((Number) params.get("userId")).longValue();
        int status = params.get("status") instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(params.get("status")));
        userAdminPersistenceService.updateUserStatus(userId, status);
        return Result.success();
    }

    @GetMapping("/artist/list")
    public Result<Map<String, Object>> getArtistList(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status
    ) {
        return Result.success(userAdminPersistenceService.listArtists(page, size, status));
    }

    @PostMapping("/artist/approve")
    public Result<Void> approveArtist(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.approveArtist(id, Objects.toString(params.get("badge"), ""));
        return Result.success();
    }

    @PostMapping("/artist/reject")
    public Result<Void> rejectArtist(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.rejectArtist(id, Objects.toString(params.get("reason"), ""));
        return Result.success();
    }

    @PostMapping("/artist/revoke")
    public Result<Void> revokeArtist(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.revokeArtist(id);
        return Result.success();
    }

    @PostMapping("/artist/reapply")
    public Result<Void> reapplyArtist(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.reapplyArtist(id);
        return Result.success();
    }

    @PostMapping("/artist/add")
    public Result<Map<String, Object>> addArtist(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = userAdminPersistenceService.addArtist(params);
        return Result.success(result);
    }

    @PostMapping("/artist/badge")
    public Result<Void> setArtistBadge(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.setArtistBadge(id, Objects.toString(params.get("badge"), ""));
        return Result.success();
    }

    @GetMapping("/promoter/list")
    public Result<Map<String, Object>> getPromoterList(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String level,
        @RequestParam(required = false) String status
    ) {
        Map<String, Object> payload = userAdminPersistenceService.listPromoters(page, size, userId, level, status).get(0);
        return Result.success(payload);
    }

    @PostMapping("/promoter/add")
    public Result<Map<String, Object>> addPromoter(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = userAdminPersistenceService.addPromoter(params);
        return Result.success(result);
    }

    @PostMapping("/promoter/freeze")
    public Result<Void> freezePromoter(@RequestBody Map<String, Object> params) {
        Long userId = Long.parseLong(String.valueOf(params.get("userId")));
        int status = params.get("status") instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(params.get("status")));
        userAdminPersistenceService.updatePromoterStatus(userId, status);
        return Result.success();
    }

    @GetMapping("/promoter/{userId}")
    public Result<Map<String, Object>> getPromoterDetail(@PathVariable Long userId) {
        Map<String, Object> detail = userAdminPersistenceService.getPromoterDetail(userId);
        if (detail == null) {
            return Result.fail(404, "艺荐官不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/promoter/team/{userId}")
    public Result<List<Map<String, Object>>> getPromoterTeam(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(userAdminPersistenceService.getPromoterTeam(userId, page, size));
    }

    @GetMapping("/promoter/commission/{userId}")
    public Result<List<Map<String, Object>>> getPromoterCommission(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(userAdminPersistenceService.getPromoterCommission(userId, page, size));
    }

    // ==================== 艺荐官认证管理 ====================

    @PostMapping("/promoter/approve")
    public Result<Void> approvePromoter(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.approvePromoter(id);
        return Result.success();
    }

    @PostMapping("/promoter/reject")
    public Result<Void> rejectPromoter(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.rejectPromoter(id, Objects.toString(params.get("reason"), ""));
        return Result.success();
    }

    @PostMapping("/promoter/revoke")
    public Result<Void> revokePromoter(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.revokePromoter(id);
        return Result.success();
    }

    @PostMapping("/promoter/reapply")
    public Result<Void> reapplyPromoter(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.reapplyPromoter(id);
        return Result.success();
    }

    @PostMapping("/promoter/level")
    public Result<Void> setPromoterLevel(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        int level = params.get("level") instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(params.get("level")));
        userAdminPersistenceService.setPromoterLevelById(id, level);
        return Result.success();
    }

    @PostMapping("/create")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> params) {
        String phone = Objects.toString(params.get("phone"), "").trim();
        String nickname = Objects.toString(params.get("nickname"), "").trim();
        
        if (phone.isEmpty()) {
            return Result.fail(400, "手机号不能为空");
        }
        
        // 检查用户是否已存在
        Long existingUserId = userAdminPersistenceService.findUserIdByPhone(phone);
        if (existingUserId != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", existingUserId);
            result.put("isNewUser", false);
            result.put("message", "用户已存在，用户ID：" + existingUserId);
            return Result.success(result);
        }
        
        // 创建新用户
        if (nickname.isEmpty()) {
            nickname = "用户" + phone.substring(phone.length() - 4);
        }
        
        Long userId = userAdminPersistenceService.createUser(phone, nickname, List.of("collector"));
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("isNewUser", true);
        result.put("message", "新用户创建成功，用户ID：" + userId);
        return Result.success(result);
    }
}
