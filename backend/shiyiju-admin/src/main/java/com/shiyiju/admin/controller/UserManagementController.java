package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.UserAdminPersistenceService;
import com.shiyiju.admin.service.AdminAccountService;
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
    private final AdminAccountService adminAccountService;

    public UserManagementController(UserAdminPersistenceService userAdminPersistenceService, AdminAccountService adminAccountService) {
        this.userAdminPersistenceService = userAdminPersistenceService;
        this.adminAccountService = adminAccountService;
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

    /**
     * 获取艺术家用户列表（users表 - 作品作者）
     */
    @GetMapping("/artistUsers")
    public Result<PageResult<Map<String, Object>>> getArtistUsersList(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status
    ) {
        return Result.success(userAdminPersistenceService.listArtistUsers(page, size, keyword, status));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats() {
        return Result.success(userAdminPersistenceService.getUserStats());
    }

    @GetMapping("/{userId}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable String userId) {
        Long numericId = parseUserId(userId);
        Map<String, Object> detail = userAdminPersistenceService.getUserDetail(numericId);
        if (detail == null) {
            return Result.fail(404, "用户不存在");
        }
        return Result.success(detail);
    }

    @PutMapping("/{userId}")
    public Result<Void> updateUser(@PathVariable String userId, @RequestBody Map<String, Object> params) {
        Long numericId = parseUserId(userId);
        userAdminPersistenceService.updateUser(numericId, params);
        return Result.success();
    }
    
    @PostMapping("/updateUid")
    public Result<Void> updateUserUid(@RequestBody Map<String, Object> params) {
        Long userId = Long.parseLong(String.valueOf(params.get("userId")));
        String uid = Objects.toString(params.get("uid"), "");
        if (uid.isEmpty()) {
            return Result.fail(400, "UID不能为空");
        }
        userAdminPersistenceService.updateUserUid(userId, uid);
        return Result.success();
    }
    
    @PostMapping("/batchUpdateUids")
    public Result<Void> batchUpdateUserUids(@RequestBody Map<String, Object> params) {
        List<Long> userIds = ((List<Number>) params.get("userIds")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        List<String> uids = ((List<?>) params.get("uids")).stream()
            .map(Object::toString)
            .collect(java.util.stream.Collectors.toList());
        if (userIds.size() != uids.size()) {
            return Result.fail(400, "用户ID数量和UID数量不匹配");
        }
        userAdminPersistenceService.batchUpdateUserUids(userIds, uids);
        return Result.success();
    }

    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@RequestBody Map<String, Object> params) {
        Long userId = ((Number) params.get("userId")).longValue();
        int status = params.get("status") instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(params.get("status")));
        userAdminPersistenceService.updateUserStatus(userId, status);
        return Result.success();
    }

    /**
     * 批量更新用户状态
     * @param params 包含 userIds（用户ID列表）和 status（目标状态）
     */
    @PostMapping("/batchUpdateStatus")
    public Result<Void> batchUpdateStatus(@RequestBody Map<String, Object> params) {
        List<Long> userIds = ((List<Number>) params.get("userIds")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        int status = params.get("status") instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(params.get("status")));
        if (userIds.isEmpty()) {
            return Result.fail(400, "用户ID列表不能为空");
        }
        userAdminPersistenceService.batchUpdateUserStatus(userIds, status);
        return Result.success();
    }

    /**
     * 批量删除用户
     * @param params 包含 userIds（用户ID列表）
     */
    @PostMapping("/batchDelete")
    public Result<Void> batchDelete(@RequestBody Map<String, Object> params) {
        List<Long> userIds = ((List<Number>) params.get("userIds")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        if (userIds.isEmpty()) {
            return Result.fail(400, "用户ID列表不能为空");
        }
        userAdminPersistenceService.batchDeleteUsers(userIds);
        return Result.success();
    }

    /**
     * 批量分配用户身份
     * @param params 包含 userIds（用户ID列表）和 identities（身份列表）
     */
    @PostMapping("/batchAssignIdentities")
    public Result<Void> batchAssignIdentities(@RequestBody Map<String, Object> params) {
        List<Long> userIds = ((List<Number>) params.get("userIds")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        List<String> identities = ((List<?>) params.get("identities")).stream()
            .map(Object::toString)
            .collect(java.util.stream.Collectors.toList());
        if (userIds.isEmpty()) {
            return Result.fail(400, "用户ID列表不能为空");
        }
        userAdminPersistenceService.batchAssignIdentities(userIds, identities);
        return Result.success();
    }

    @GetMapping("/artist/list")
    public Result<Map<String, Object>> getArtistList(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String badge,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(required = false) String sortField,
        @RequestParam(required = false) String sortOrder
    ) {
        return Result.success(userAdminPersistenceService.listArtists(page, size, status, keyword, phone, userId, badge, startDate, endDate, sortField, sortOrder));
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
    public Result<Void> revokeArtist(
        @RequestBody Map<String, Object> params,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!isAdmin(authorization)) {
            return Result.fail(403, "需要管理员权限");
        }
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.revokeArtist(id);
        return Result.success();
    }

    @PostMapping("/artist/hide")
    public Result<Void> hideArtist(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        userAdminPersistenceService.hideArtist(id);
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
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(required = false) String sortField,
        @RequestParam(required = false) String sortOrder
    ) {
        return Result.success(userAdminPersistenceService.listPromoters(page, size, userId, level, status, keyword, phone, startDate, endDate, sortField, sortOrder));
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

    // ==================== 艺荐官批量操作 ====================

    @PostMapping("/promoter/batchApprove")
    public Result<Void> batchApprovePromoter(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) params.get("ids")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        if (ids.isEmpty()) {
            return Result.fail(400, "请选择要操作的艺荐官");
        }
        userAdminPersistenceService.batchApprovePromoters(ids);
        return Result.success();
    }

    @PostMapping("/promoter/batchReject")
    public Result<Void> batchRejectPromoter(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) params.get("ids")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        String reason = Objects.toString(params.get("reason"), "不符合条件");
        if (ids.isEmpty()) {
            return Result.fail(400, "请选择要操作的艺荐官");
        }
        userAdminPersistenceService.batchRejectPromoters(ids, reason);
        return Result.success();
    }

    @PostMapping("/promoter/batchDelete")
    public Result<Void> batchDeletePromoter(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) params.get("ids")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        if (ids.isEmpty()) {
            return Result.fail(400, "请选择要删除的艺荐官");
        }
        userAdminPersistenceService.batchDeletePromoters(ids);
        return Result.success();
    }

    @GetMapping("/promoter/{userId}")
    public Result<Map<String, Object>> getPromoterDetail(@PathVariable String userId) {
        Long numericId = parseUserId(userId);
        Map<String, Object> detail = userAdminPersistenceService.getPromoterDetail(numericId);
        if (detail == null) {
            return Result.fail(404, "艺荐官不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/promoter/team/{userId}")
    public Result<List<Map<String, Object>>> getPromoterTeam(
        @PathVariable String userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long numericId = parseUserId(userId);
        return Result.success(userAdminPersistenceService.getPromoterTeam(numericId, page, size));
    }

    @GetMapping("/promoter/commission/{userId}")
    public Result<List<Map<String, Object>>> getPromoterCommission(
        @PathVariable String userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long numericId = parseUserId(userId);
        return Result.success(userAdminPersistenceService.getPromoterCommission(numericId, page, size));
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

    @PutMapping("/promoter/{userId}/relation")
    public Result<Void> updatePromoterRelation(@PathVariable String userId, @RequestBody Map<String, Object> params) {
        Long numericId = parseUserId(userId);
        userAdminPersistenceService.updatePromoterRelation(numericId, params);
        return Result.success();
    }

    @GetMapping("/{userId}/artworks")
    public Result<Map<String, Object>> getUserArtworks(
        @PathVariable String userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long numericId = parseUserId(userId);
        return Result.success(userAdminPersistenceService.listUserArtworks(numericId, page, size));
    }

    @GetMapping("/{userId}/sales")
    public Result<Map<String, Object>> getUserSales(
        @PathVariable String userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long numericId = parseUserId(userId);
        return Result.success(userAdminPersistenceService.listUserSales(numericId, page, size));
    }

    @GetMapping("/{userId}/sharing")
    public Result<Map<String, Object>> getUserSharing(
        @PathVariable String userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long numericId = parseUserId(userId);
        return Result.success(userAdminPersistenceService.listUserSharing(numericId, page, size));
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

    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable String userId) {
        Long numericId = parseUserId(userId);
        userAdminPersistenceService.deleteUser(numericId);
        return Result.success();
    }

    @DeleteMapping("/artist/{id}")
    public Result<Void> deleteArtist(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!isAdmin(authorization)) {
            return Result.fail(403, "需要管理员权限");
        }
        userAdminPersistenceService.deleteArtist(id);
        return Result.success();
    }

    /**
     * 批量审核艺术家 - 通过
     * @param params 包含 ids（艺术家记录ID列表）和 badge（认证等级，可选）
     */
    @PostMapping("/artist/batchApprove")
    public Result<Void> batchApproveArtist(@RequestBody Map<String, Object> params) {
        List<Long> ids = ((List<Number>) params.get("ids")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        String badge = Objects.toString(params.get("badge"), "");
        if (ids.isEmpty()) {
            return Result.fail(400, "艺术家ID列表不能为空");
        }
        userAdminPersistenceService.batchApproveArtist(ids, badge);
        return Result.success();
    }

    /**
     * 批量审核艺术家 - 拒绝
     * @param params 包含 ids（艺术家记录ID列表）和 reason（拒绝原因）
     */
    @PostMapping("/artist/batchReject")
    public Result<Void> batchRejectArtist(@RequestBody Map<String, Object> params) {
        List<Long> ids = ((List<Number>) params.get("ids")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        String reason = Objects.toString(params.get("reason"), "");
        if (ids.isEmpty()) {
            return Result.fail(400, "艺术家ID列表不能为空");
        }
        userAdminPersistenceService.batchRejectArtist(ids, reason);
        return Result.success();
    }

    /**
     * 批量隐藏艺术家
     * @param params 包含 ids（艺术家记录ID列表）
     */
    @PostMapping("/artist/batchHide")
    public Result<Void> batchHideArtist(@RequestBody Map<String, Object> params) {
        List<Long> ids = ((List<Number>) params.get("ids")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        if (ids.isEmpty()) {
            return Result.fail(400, "艺术家ID列表不能为空");
        }
        userAdminPersistenceService.batchHideArtist(ids);
        return Result.success();
    }

    /**
     * 批量删除艺术家
     * @param params 包含 ids（艺术家记录ID列表）
     */
    @PostMapping("/artist/batchDelete")
    public Result<Void> batchDeleteArtist(
        @RequestBody Map<String, Object> params,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!isAdmin(authorization)) {
            return Result.fail(403, "需要管理员权限");
        }
        List<Long> ids = ((List<Number>) params.get("ids")).stream()
            .map(Number::longValue)
            .collect(java.util.stream.Collectors.toList());
        if (ids.isEmpty()) {
            return Result.fail(400, "艺术家ID列表不能为空");
        }
        userAdminPersistenceService.batchDeleteArtist(ids);
        return Result.success();
    }

    @DeleteMapping("/promoter/{id}")
    public Result<Void> deletePromoter(@PathVariable Long id) {
        userAdminPersistenceService.deletePromoter(id);
        return Result.success();
    }

    /**
     * 解析用户ID，支持数字ID和字符串UID
     * @param userIdStr 用户ID字符串
     * @return 数字用户ID
     */
    private Long parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        // 如果是纯数字，直接返回
        if (userIdStr.matches("\\d+")) {
            return Long.parseLong(userIdStr);
        }
        // 如果是字符串UID，查询对应的数字ID
        return userAdminPersistenceService.getUserIdByUid(userIdStr);
    }

    private boolean isAdmin(String authorization) {
        if (authorization == null || !authorization.replace("Bearer ", "").startsWith("admin:")) {
            return false;
        }
        Map<String, Object> admin = adminAccountService.getAdminInfo(authorization);
        return Objects.equals(admin.get("role"), "super") || Objects.equals(admin.get("role"), "admin");
    }
}
