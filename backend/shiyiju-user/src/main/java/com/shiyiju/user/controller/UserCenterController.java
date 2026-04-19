package com.shiyiju.user.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.common.entity.Address;
import com.shiyiju.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 个人中心控制器
 * 处理用户个人中心、收货地址、收藏、足迹等接口
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserService userService;

    /**
     * 获取个人中心聚合数据 (GET /user/center)
     */
    @GetMapping("/center")
    public Result<Map<String, Object>> getUserCenter(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(userService.getUserCenter(userId));
    }

    /**
     * 获取收货地址列表 (GET /user/addresses)
     */
    @GetMapping("/addresses")
    public Result<List<Address>> getAddresses(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(userService.getAddressList(userId));
    }

    /**
     * 添加收货地址 (POST /user/addresses)
     */
    @PostMapping("/addresses")
    public Result<Void> addAddress(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Address address
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.addAddress(userId, address);
        return Result.success();
    }

    /**
     * 更新收货地址 (PUT /user/addresses/{id})
     */
    @PutMapping("/addresses/{id}")
    public Result<Void> updateAddress(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @RequestBody Address address
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.updateAddress(id, userId, address);
        return Result.success();
    }

    /**
     * 删除收货地址 (DELETE /user/addresses/{id})
     */
    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.deleteAddress(id, userId);
        return Result.success();
    }

    /**
     * 我的收藏列表 (GET /user/favorites)
     */
    @GetMapping("/favorites")
    public Result<Map<String, Object>> getMyFavorites(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        // TODO: 调用商品服务获取收藏列表
        return Result.success(Map.of(
                "list", List.of(),
                "total", 0,
                "page", page,
                "pageSize", pageSize
        ));
    }

    /**
     * 浏览足迹 (GET /user/history)
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> getMyHistory(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        // TODO: 获取浏览足迹列表
        return Result.success(Map.of(
                "list", List.of(),
                "total", 0,
                "page", page,
                "pageSize", pageSize
        ));
    }
}
