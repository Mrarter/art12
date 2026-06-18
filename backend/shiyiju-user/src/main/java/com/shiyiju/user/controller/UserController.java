package com.shiyiju.user.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.user.dto.ArtistCertDTO;
import com.shiyiju.user.dto.RegisterDTO;
import com.shiyiju.user.dto.WxLoginDTO;
import com.shiyiju.user.entity.User;
import com.shiyiju.user.service.UserService;
import com.shiyiju.user.vo.LoginVO;
import com.shiyiju.user.vo.UserInfoVO;
import com.shiyiju.user.vo.UserInteractionStatsVO;
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
     * 刷新 Token (POST /user/auth/refresh)
     * 用于 Token 即将过期时无感刷新
     */
    @PostMapping("/auth/refresh")
    public Result<LoginVO> refreshToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.fail(401, "无效的 Token");
        }
        String token = authHeader.substring(7);
        log.info("Token 刷新请求, token: {}", token.substring(0, Math.min(20, token.length())));
        LoginVO vo = userService.refreshToken(token);
        if (vo == null) {
            return Result.fail(401, "Token 已过期，请重新登录");
        }
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
     * 更新艺术家主页版式 (PUT /user/artist/homepage-style)
     */
    @PutMapping("/artist/homepage-style")
    public Result<Void> updateArtistHomepageStyle(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        Object style = params.getOrDefault("style", params.get("homepageStyle"));
        userService.updateArtistHomepageStyle(userId, style == null ? null : String.valueOf(style));
        return Result.success();
    }

    /**
     * 更新艺术家履历 (PUT /user/artist/resume)
     */
    @PutMapping("/artist/resume")
    public Result<Void> updateArtistResume(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        Object resume = params.get("resume");
        userService.updateArtistResume(userId, resume == null ? "" : String.valueOf(resume));
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
    public Result<Map<String, Object>> getArtistHomepage(
            @PathVariable Long artistId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId
    ) {
        Map<String, Object> data = userService.getArtistHomepage(artistId);
        if (currentUserId != null) {
            boolean following = userService.isFollowing(currentUserId, artistId);
            data.put("isFollowing", following);
            data.put("followed", following);
            data.put("isOwner", currentUserId.equals(artistId));
        }
        return Result.success(data);
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
    public Result<java.util.Map<String, Object>> getArtistInfo(
            @PathVariable Long artistId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId
    ) {
        java.util.Map<String, Object> data = userService.getArtistInfo(artistId);
        if (data == null) {
            return Result.fail(404, "艺术家不存在");
        }
        if (currentUserId != null) {
            boolean following = userService.isFollowing(currentUserId, artistId);
            data.put("isFollowing", following);
            data.put("followed", following);
            data.put("isOwner", currentUserId.equals(artistId));
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
    public Result<Void> updateUid(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody java.util.Map<String, Object> params) {
        if (authorization == null && params.get("adminKey") == null) {
            return Result.fail(401, "需要管理员权限");
        }
        Long userId = ((Number) params.get("userId")).longValue();
        String uid = params.get("uid").toString();
        userService.updateUid(userId, uid);
        return Result.success();
    }

    // ===================== 实名认证 API =====================

    /**
     * 提交实名认证申请 (POST /user/realname/submit)
     */
    @PostMapping("/user/realname/submit")
    public Result<Void> submitRealnameCert(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody com.shiyiju.user.dto.RealnameCertSubmitDTO dto) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.submitRealnameCert(userId, dto);
        return Result.success();
    }

    /**
     * 查询实名认证状态 (GET /user/realname/status)
     */
    @GetMapping("/user/realname/status")
    public Result<com.shiyiju.user.vo.RealnameCertStatusVO> getRealnameCertStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(userService.getRealnameCertStatus(userId));
    }

    /**
     * 校验用户真实互动数据 (GET /user/interaction/stats)
     * 从数据库精准聚合关注数、收藏数、点赞数，排除虚假/无效记录
     *
     * @param userId 用户 ID（必填）
     */
    @GetMapping("/interaction/stats")
    public Result<UserInteractionStatsVO> verifyInteractionStats(@RequestParam Long userId) {
        if (userId == null || userId <= 0) {
            return Result.fail(400, "无效的用户 ID");
        }
        UserInteractionStatsVO vo = userService.verifyInteractionStats(userId);
        return Result.success(vo);
    }

    /**
     * 用户注册 (POST /user/register)
     * 手机号 + 验证码注册
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        log.info("用户注册请求, phone: {}", dto.getPhone());
        LoginVO vo = userService.register(dto);
        return Result.success(vo);
    }

    /**
     * 手机号登录 (POST /user/phone-login)
     * 手机号 + 验证码登录（已注册用户）
     */
    @PostMapping("/phone-login")
    public Result<LoginVO> phoneLogin(@Valid @RequestBody RegisterDTO dto) {
        log.info("手机号登录请求, phone: {}", dto.getPhone());
        LoginVO vo = userService.phoneLogin(dto);
        return Result.success(vo);
    }

    /**
     * 密码登录 (POST /user/password-login)
     * 手机号 + 密码登录（已注册用户）
     */
    @PostMapping("/password-login")
    public Result<LoginVO> passwordLogin(@RequestBody RegisterDTO dto) {
        log.info("密码登录请求, phone: {}", dto.getPhone());
        LoginVO vo = userService.passwordLogin(dto);
        return Result.success(vo);
    }

    /**
     * 发送短信验证码 (POST /user/sms-code)
     */
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String type = params.get("type");
        log.info("发送短信验证码请求, phone: {}, type: {}", phone, type);
        userService.sendSmsCode(phone, type);
        return Result.success();
    }

}
