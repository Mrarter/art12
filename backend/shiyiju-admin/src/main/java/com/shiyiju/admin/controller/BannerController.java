package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 轮播图管理控制器
 */
@RestController
@RequestMapping("/admin/banner")
public class BannerController {

    /**
     * 轮播图列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getBannerList(
            @RequestParam(required = false) String type) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        String[] titles = {"首页Banner", "艺术家推荐", "拍卖专场"};
        String[] types = {"BANNER", "ARTIST", "AUCTION"};
        
        for (int i = 0; i < titles.length; i++) {
            Map<String, Object> banner = new LinkedHashMap<>();
            banner.put("id", i + 1);
            banner.put("title", titles[i]);
            banner.put("type", types[i]);
            banner.put("imageUrl", "/uploads/images/banner" + (i + 1) + ".jpg");
            banner.put("target", "跳转链接" + (i + 1));
            banner.put("sortNo", 10 - i);
            banner.put("status", i == 0 ? "ENABLED" : "DISABLED");
            banner.put("createTime", "2024-04-" + String.format("%02d", 10 + i) + " 10:30:00");
            list.add(banner);
        }
        return Result.success(list);
    }

    /**
     * 创建轮播图
     */
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return Result.success(System.currentTimeMillis());
    }

    /**
     * 更新轮播图
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return Result.success();
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return Result.success();
    }

    /**
     * 更新轮播图状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return Result.success();
    }
}