package com.shiyiju.user.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.user.dto.PayAccountAddDTO;
import com.shiyiju.user.dto.WxLoginDTO;
import com.shiyiju.user.service.PayAccountService;
import com.shiyiju.user.service.UserService;
import com.shiyiju.user.vo.PayAccountVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收款账户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user/pay-account")
@RequiredArgsConstructor
public class PayAccountController {

    private final PayAccountService payAccountService;
    private final UserService userService;

    /** 添加收款账户 (POST /user/pay-account/add) */
    @PostMapping("/add")
    public Result<Void> addAccount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody PayAccountAddDTO dto) {
        if (userId == null) return Result.fail(401, "请先登录");
        payAccountService.addAccount(userId, dto);
        return Result.success();
    }

    /** 绑定当前登录账号的微信收款账户 (POST /user/pay-account/bind-wechat) */
    @PostMapping("/bind-wechat")
    public Result<PayAccountVO> bindWechat(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody(required = false) Map<String, Object> params) {
        if (userId == null) return Result.fail(401, "请先登录");
        Boolean setDefault = params != null && Boolean.TRUE.equals(params.get("setDefault"));
        return Result.success(payAccountService.bindCurrentWechat(userId, setDefault));
    }

    /** 微信授权后绑定当前登录账号 (POST /user/pay-account/bind-wechat-code) */
    @PostMapping("/bind-wechat-code")
    public Result<PayAccountVO> bindWechatByCode(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody WxLoginDTO dto) {
        if (userId == null) return Result.fail(401, "请先登录");
        userService.bindWechatToCurrentUser(userId, dto);
        return Result.success(payAccountService.bindCurrentWechat(userId, dto.getSetDefault()));
    }

    /** 获取账户列表 (GET /user/pay-account/list) */
    @GetMapping("/list")
    public Result<List<PayAccountVO>> getAccountList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.success(payAccountService.getAccountList(userId));
    }

    /** 删除账户 (POST /user/pay-account/delete) */
    @PostMapping("/delete")
    public Result<Void> deleteAccount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params) {
        if (userId == null) return Result.fail(401, "请先登录");
        Long accountId = ((Number) params.get("id")).longValue();
        payAccountService.deleteAccount(userId, accountId);
        return Result.success();
    }

    /** 设置默认账户 (POST /user/pay-account/default) */
    @PostMapping("/default")
    public Result<Void> setDefaultAccount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params) {
        if (userId == null) return Result.fail(401, "请先登录");
        Long accountId = ((Number) params.get("id")).longValue();
        payAccountService.setDefaultAccount(userId, accountId);
        return Result.success();
    }

    /** 获取默认账户 (GET /user/pay-account/default) */
    @GetMapping("/default")
    public Result<PayAccountVO> getDefaultAccount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.success(payAccountService.getDefaultAccount(userId));
    }
}
