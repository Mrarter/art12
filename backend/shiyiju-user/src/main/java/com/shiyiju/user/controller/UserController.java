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
import org.springframework.web.bind.annotation.*;

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
     * 获取用户信息 (GET /user/info)
     */
    @GetMapping("/user/info")
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
            @RequestBody ArtistCertDTO dto
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
    @GetMapping("/user/artist/cert/status")
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
     * 获取艺术家主页信息 (GET /artist/{userId})
     */
    @GetMapping("/artist/{artistId}")
    public Result<Map<String, Object>> getArtistHomepage(@PathVariable Long artistId) {
        return Result.success(userService.getArtistHomepage(artistId));
    }
}
