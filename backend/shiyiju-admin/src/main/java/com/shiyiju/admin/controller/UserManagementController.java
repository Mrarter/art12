package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/user")
public class UserManagementController {

    /**
     * 用户列表
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("userId", "1000" + i);
            user.put("nickname", "用户" + i);
            user.put("phone", "13800138" + String.format("%03d", i));
            user.put("avatar", "");
            user.put("isArtist", i % 2 == 0);
            user.put("isPromoter", i % 3 == 0);
            user.put("balance", 1000 * i);
            user.put("orderCount", 5 * i);
            user.put("createTime", "2024-01-" + String.format("%02d", 15 + i) + " 10:30:00");
            user.put("status", 1);
            list.add(user);
        }
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(3L);
        result.setPage(page);
        result.setPageSize(size);
        return Result.success(result);
    }

    /**
     * 艺术家认证列表
     */
    @GetMapping("/artist/list")
    public Result<PageResult<Map<String, Object>>> getArtistList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> artist = new HashMap<>();
        artist.put("id", 1);
        artist.put("nickname", "艺术家A");
        artist.put("phone", "13800138001");
        artist.put("realName", "张伟");
        artist.put("idCard", "110101199001011234");
        artist.put("status", status != null ? status : "pending");
        artist.put("bio", "毕业于中央美术学院");
        artist.put("images", new ArrayList<>());
        artist.put("createTime", "2024-01-20 10:00:00");
        list.add(artist);
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(1L);
        return Result.success(result);
    }

    /**
     * 通过艺术家认证
     */
    @PostMapping("/artist/approve")
    public Result<Void> approveArtist(@RequestBody Map<String, Object> params) {
        return Result.success();
    }

    /**
     * 拒绝艺术家认证
     */
    @PostMapping("/artist/reject")
    public Result<Void> rejectArtist(@RequestBody Map<String, Object> params) {
        return Result.success();
    }

    /**
     * 艺荐官列表
     */
    @GetMapping("/promoter/list")
    public Result<Map<String, Object>> getPromoterList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 1256);
        stats.put("monthlyNew", 89);
        stats.put("pendingCommission", 258000);
        
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> promoter = new HashMap<>();
        promoter.put("userId", "20001");
        promoter.put("nickname", "艺荐官A");
        promoter.put("level", 3);
        promoter.put("teamCount", 156);
        promoter.put("directCount", 28);
        promoter.put("totalCommission", 125000);
        promoter.put("withdrawable", 8500);
        promoter.put("status", 1);
        promoter.put("becomeTime", "2023-06-01 10:00:00");
        list.add(promoter);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("stats", stats);
        return Result.success(result);
    }

    /**
     * 更新用户状态
     */
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@RequestBody Map<String, Object> params) {
        return Result.success();
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    public Result<Void> deleteUser(@RequestBody Map<String, Object> params) {
        return Result.success();
    }
}
