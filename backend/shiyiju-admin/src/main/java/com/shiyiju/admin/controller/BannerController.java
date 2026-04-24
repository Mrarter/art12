package com.shiyiju.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理员 - 轮播图管理控制器
 */
@RestController
@RequestMapping("/admin/banner")
public class BannerController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 轮播图列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getBannerList(
            @RequestParam(required = false) String type) {
        
        List<Map<String, Object>> banners = jdbcTemplate.queryForList(
            "SELECT id, title, image_url, link_type, link_value, sort, status, create_time FROM banner ORDER BY sort DESC, id DESC"
        );
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> b : banners) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", b.get("id"));
            item.put("title", b.get("title"));
            item.put("imageUrl", b.get("image_url"));
            item.put("type", b.get("link_type"));
            item.put("target", b.get("link_value"));
            item.put("sortNo", b.get("sort"));
            item.put("status", b.get("status") != null && (Integer)b.get("status") == 1 ? "ENABLED" : "DISABLED");
            item.put("createTime", b.get("create_time"));
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 创建轮播图
     */
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        String title = (String) params.get("title");
        String imageUrl = (String) params.get("imageUrl");
        String linkType = (String) params.getOrDefault("type", "BANNER");
        String linkValue = (String) params.getOrDefault("target", "");
        Integer sort = params.get("sortNo") != null ? ((Number) params.get("sortNo")).intValue() : 0;
        String status = (String) params.getOrDefault("status", "ENABLED");
        
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT INTO banner (title, image_url, link_type, link_value, sort, status, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            title, imageUrl, linkType, linkValue, sort, "ENABLED".equals(status) ? 1 : 0, now, now
        );
        
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Result.success(id);
    }

    /**
     * 更新轮播图
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("UPDATE banner SET update_time = ?");
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        
        if (params.containsKey("title")) {
            sql.append(", title = ?");
            args.add(params.get("title"));
        }
        if (params.containsKey("imageUrl")) {
            sql.append(", image_url = ?");
            args.add(params.get("imageUrl"));
        }
        if (params.containsKey("type")) {
            sql.append(", link_type = ?");
            args.add(params.get("type"));
        }
        if (params.containsKey("target")) {
            sql.append(", link_value = ?");
            args.add(params.get("target"));
        }
        if (params.containsKey("sortNo")) {
            sql.append(", sort = ?");
            args.add(((Number) params.get("sortNo")).intValue());
        }
        if (params.containsKey("status")) {
            sql.append(", status = ?");
            args.add("ENABLED".equals(params.get("status")) ? 1 : 0);
        }
        
        sql.append(" WHERE id = ?");
        args.add(id);
        
        jdbcTemplate.update(sql.toString(), args.toArray());
        return Result.success();
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM banner WHERE id = ?", id);
        return Result.success();
    }

    /**
     * 更新轮播图状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        String status = (String) params.get("status");
        int statusVal = "ENABLED".equals(status) ? 1 : 0;
        jdbcTemplate.update("UPDATE banner SET status = ?, update_time = ? WHERE id = ?", 
            statusVal, LocalDateTime.now(), id);
        return Result.success();
    }
}