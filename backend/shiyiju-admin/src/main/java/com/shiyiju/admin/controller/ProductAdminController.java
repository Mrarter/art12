package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 作品/商品管理控制器
 */
@RestController
@RequestMapping("/admin/product")
public class ProductAdminController {

    /**
     * 作品列表
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getProductList(
            @RequestParam(required = false) Long artworkId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artistName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        // 模拟数据，实际应从商品服务获取
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("artworkId", 19 + i);
            product.put("artworkCode", "SYJ" + String.format("%06d", 19 + i));
            product.put("title", "作品" + i);
            product.put("artistName", "艺术家" + i);
            product.put("categoryName", "国画");
            product.put("cover", "http://example.com/image.jpg");
            product.put("price", 5000.00 + i * 1000);
            product.put("originalPrice", 6000.00 + i * 1000);
            product.put("stock", 10 + i);
            product.put("status", status != null ? status : 1);
            product.put("statusText", "上架中");
            product.put("createTime", "2024-04-" + String.format("%02d", 10 + i) + " 10:30:00");
            product.put("saleCount", 5 + i);
            product.put("favoriteCount", 20 + i * 5);
            list.add(product);
        }
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(20L);
        result.setPage(pageNum);
        result.setPageSize(pageSize);
        return Result.success(result);
    }

    /**
     * 作品分类列表
     */
    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> getCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();
        String[] names = {"国画", "油画", "书法", "版画", "雕塑", "装置", "装置", "其他"};
        for (int i = 1; i <= names.length; i++) {
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("id", i);
            cat.put("name", names[i - 1]);
            cat.put("sort", 10 - i);
            cat.put("status", 1);
            categories.add(cat);
        }
        return Result.success(categories);
    }

    /**
     * 作品审核列表
     */
    @GetMapping("/audit/list")
    public Result<PageResult<Map<String, Object>>> getAuditList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", i);
            item.put("artworkId", 100 + i);
            item.put("title", "待审核作品" + i);
            item.put("artistName", "申请艺术家" + i);
            item.put("categoryName", "国画");
            item.put("cover", "http://example.com/image.jpg");
            item.put("applyTime", "2024-04-" + String.format("%02d", 15 + i) + " 10:30:00");
            item.put("status", status != null ? status : 0);
            item.put("statusText", status != null && status == 1 ? "已通过" : "待审核");
            list.add(item);
        }
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(5L);
        result.setPage(page);
        result.setPageSize(size);
        return Result.success(result);
    }

    /**
     * 上传作品图片
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") Object file) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", "http://example.com/upload/" + System.currentTimeMillis() + ".jpg");
        result.put("filename", "image.jpg");
        return Result.success(result);
    }

    /**
     * 创建作品
     */
    @PostMapping("/create")
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        return Result.success();
    }

    /**
     * 更新作品
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Map<String, Object> params) {
        return Result.success();
    }

    /**
     * 删除作品
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return Result.success();
    }
}