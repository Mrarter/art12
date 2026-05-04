package com.shiyiju.user.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.user.dto.ArtistCertDTO;
import com.shiyiju.user.dto.WxLoginDTO;
import com.shiyiju.user.entity.User;
import com.shiyiju.user.service.UserService;
import com.shiyiju.user.vo.LoginVO;
import com.shiyiju.user.vo.UserInfoVO;
import com.shiyiju.user.vo.ArtistCertStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 微信登录 (POST /auth/wx-login)
     */
    @PostMapping("/auth/wx-login")
    public Result<LoginVO> wxLogin(@RequestBody WxLoginDTO dto) {
        log.info("微信登录请求, code: {}", dto.getCode());
        LoginVO vo = userService.wxLogin(dto);
        return Result.success(vo);
    }

    /**
     * 微信登录兼容路径 (POST /user/wxlogin) - 兼容前端
     */
    @PostMapping("/wxlogin")
    public Result<LoginVO> wxLoginLegacy(@RequestBody WxLoginDTO dto) {
        log.info("微信登录请求(兼容), code: {}", dto.getCode());
        LoginVO vo = userService.wxLogin(dto);
        return Result.success(vo);
    }

    /**
     * 获取用户信息 (GET /user/info)
     */
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        UserInfoVO vo = userService.getUserInfo(userId);
        return Result.success(vo);
    }

    /**
     * 更新用户信息 (PUT /user/update)
     */
    @PutMapping("/user/update")
    public Result<Void> updateUserInfo(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody User userUpdate
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.updateUserInfo(userId, userUpdate);
        return Result.success();
    }

    /**
     * 绑定手机号 (POST /user/bind-phone)
     */
    @PostMapping("/user/bind-phone")
    public Result<Void> bindPhone(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        String phone = params.get("phone");
        String verifyCode = params.get("verifyCode");
        userService.bindPhone(userId, phone, verifyCode);
        return Result.success();
    }

    /**
     * 艺术家认证申请 (POST /user/artist/cert)
     */
    @PostMapping("/user/artist/cert")
    public Result<Void> applyArtistCert(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ArtistCertDTO dto
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.applyArtistCert(userId, dto);
        return Result.success();
    }

    /**
     * 获取艺术家认证状态 (GET /user/artist/cert/status)
     */
    @GetMapping("/artist/cert/status")
    public Result<ArtistCertStatusVO> getArtistCertStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(userService.getArtistCertStatus(userId));
    }

    /**
     * 开通艺荐官
     */
    @PostMapping("/user/promoter/open")
    public Result<Void> openPromoter(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.openPromoter(userId);
        return Result.success();
    }

    /**
     * 获取艺荐官邀请码
     */
    @GetMapping("/user/promoter/invite-code")
    public Result<String> getPromoterInviteCode(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        String inviteCode = userService.getPromoterInviteCode(userId);
        return Result.success(inviteCode);
    }

    /**
     * 关注艺术家 (POST /artist/{userId}/follow)
     */
    @PostMapping("/artist/{artistId}/follow")
    public Result<Void> followArtist(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long artistId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.followArtist(userId, artistId);
        return Result.success();
    }

    /**
     * 取消关注艺术家
     */
    @DeleteMapping("/artist/{artistId}/follow")
    public Result<Void> unfollowArtist(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long artistId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.unfollowArtist(userId, artistId);
        return Result.success();
    }

    /**
     * 搜索艺术家 (GET /user/artist/search)
     * 根据艺术家名称模糊搜索已认证的艺术家
     * 注意：此路由必须在 /artist/{artistId} 之前定义，避免 search 被当作 ID 处理
     */
    @GetMapping("/artist/search")
    public Result<java.util.List<java.util.Map<String, Object>>> searchArtists(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return Result.success(userService.searchArtists(keyword, limit));
    }

    /**
     * 获取艺术家主页信息 (GET /artist/{userId})
     */
    @GetMapping("/artist/{artistId}")
    public Result<Map<String, Object>> getArtistHomepage(@PathVariable Long artistId) {
        return Result.success(userService.getArtistHomepage(artistId));
    }

    /**
     * 根据名称查找或创建艺术家 (GET /user/artist/find-or-create)
     * 如果艺术家存在则返回，不存在则创建未审核状态的艺术家
     */
    @GetMapping("/artist/find-or-create")
    public Result<java.util.Map<String, Object>> findOrCreateArtist(@RequestParam String name) {
        return Result.success(userService.findOrCreateArtist(name));
    }

    /**
     * 获取艺术家详细信息 (GET /user/artist/info/{id})
     * 用于作品服务关联艺术家信息
     */
    @GetMapping("/artist/info/{artistId}")
    public Result<java.util.Map<String, Object>> getArtistInfo(@PathVariable Long artistId) {
        java.util.Map<String, Object> data = userService.getArtistInfo(artistId);
        if (data == null) {
            return Result.fail(404, "艺术家不存在");
        }
        return Result.success(data);
    }

    /**
     * 搜索全局用户列表 (GET /user/search)
     * 用于发布作品时选择作者，搜索所有用户
     */
    @GetMapping("/search")
    public Result<java.util.List<java.util.Map<String, Object>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return Result.success(userService.searchUsers(keyword, limit));
    }
    
    /**
     * 批量更新用户UID (POST /user/admin/batch-update-uids)
     * @param params 包含 userIds 和 uids 列表
     */
    @PostMapping("/admin/batch-update-uids")
    public Result<Void> batchUpdateUids(@RequestBody java.util.Map<String, Object> params) {
        java.util.List<?> userIdList = (java.util.List<?>) params.get("userIds");
        java.util.List<?> uidList = (java.util.List<?>) params.get("uids");
        
        if (userIdList == null || uidList == null || userIdList.size() != uidList.size()) {
            return Result.fail(400, "用户ID和UID列表不匹配");
        }
        
        java.util.List<Long> userIds = userIdList.stream()
            .map(o -> ((Number) o).longValue())
            .collect(java.util.stream.Collectors.toList());
        java.util.List<String> uids = uidList.stream()
            .map(Object::toString)
            .collect(java.util.stream.Collectors.toList());
        
        userService.batchUpdateUids(userIds, uids);
        return Result.success();
    }
    
    /**
     * 更新单个用户UID (POST /user/admin/update-uid)
     * @param params 包含 userId 和 uid
     */
    @PostMapping("/admin/update-uid")
    public Result<Void> updateUid(@RequestBody java.util.Map<String, Object> params) {
        Long userId = ((Number) params.get("userId")).longValue();
        String uid = params.get("uid").toString();
        userService.updateUid(userId, uid);
        return Result.success();
    }
}
