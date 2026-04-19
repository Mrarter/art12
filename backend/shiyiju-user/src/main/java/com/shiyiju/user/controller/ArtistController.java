package com.shiyiju.user.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.common.vo.ArtworkVO;
import com.shiyiju.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 艺术家主页控制器
 * 处理艺术家主页、作品列表、动态、关注等接口
 */
@Slf4j
@RestController
@RequestMapping("/artist")
@RequiredArgsConstructor
public class ArtistController {

    private final UserService userService;

    /**
     * 获取艺术家主页信息 (GET /artist/{userId})
     */
    @GetMapping("/{userId}")
    public Result<Map<String, Object>> getArtistHomepage(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId
    ) {
        Map<String, Object> homepage = userService.getArtistHomepage(userId);
        
        // 如果有当前登录用户，检查是否已关注
        if (currentUserId != null) {
            homepage.put("isFollowing", userService.isFollowing(currentUserId, userId));
        }
        
        return Result.success(homepage);
    }

    /**
     * 获取艺术家作品列表 (GET /artist/{userId}/works)
     */
    @GetMapping("/{userId}/works")
    public Result<PageResult<Map<String, Object>>> getArtistWorks(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        return Result.success(userService.getArtistWorks(userId, page, pageSize));
    }

    /**
     * 获取艺术家动态 (GET /artist/{userId}/moments)
     */
    @GetMapping("/{userId}/moments")
    public Result<PageResult<Map<String, Object>>> getArtistMoments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        return Result.success(userService.getArtistMoments(userId, page, pageSize));
    }

    /**
     * 关注艺术家 (POST /artist/{userId}/follow)
     */
    @PostMapping("/{userId}/follow")
    public Result<Void> followArtist(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId
    ) {
        if (currentUserId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.followArtist(currentUserId, userId);
        return Result.success();
    }

    /**
     * 取消关注艺术家 (DELETE /artist/{userId}/follow)
     */
    @DeleteMapping("/{userId}/follow")
    public Result<Void> unfollowArtist(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId
    ) {
        if (currentUserId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.unfollowArtist(currentUserId, userId);
        return Result.success();
    }
}
