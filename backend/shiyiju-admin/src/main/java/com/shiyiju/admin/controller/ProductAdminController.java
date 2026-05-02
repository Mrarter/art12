package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.ProductAdminPersistenceService;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员 - 作品分类与审核控制器
 */
@RestController
@RequestMapping("/admin/product")
public class ProductAdminController {

    private final ProductAdminPersistenceService productAdminPersistenceService;

    public ProductAdminController(ProductAdminPersistenceService productAdminPersistenceService) {
        this.productAdminPersistenceService = productAdminPersistenceService;
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> getCategories() {
        return Result.success(productAdminPersistenceService.listCategories());
    }

    @PostMapping("/categories")
    public Result<Long> createCategory(@RequestBody Map<String, Object> params) {
        return Result.success(productAdminPersistenceService.createCategory(params));
    }

    @PutMapping("/categories/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        productAdminPersistenceService.updateCategory(id, params);
        return Result.success();
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        productAdminPersistenceService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/audit/list")
    public Result<Map<String, Object>> getAuditList(
        @RequestParam(required = false) String auditStatus,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(productAdminPersistenceService.listAuditRecords(auditStatus, page, size));
    }

    @PostMapping("/audit/approve")
    public Result<Void> approveArtwork(@RequestBody Map<String, Object> params) {
        Long artworkId = ((Number) params.get("artworkId")).longValue();
        productAdminPersistenceService.approveArtwork(artworkId);
        return Result.success();
    }

    @PostMapping("/audit/reject")
    public Result<Void> rejectArtwork(@RequestBody Map<String, Object> params) {
        Long artworkId = ((Number) params.get("artworkId")).longValue();
        productAdminPersistenceService.rejectArtwork(artworkId, String.valueOf(params.get("reason")));
        return Result.success();
    }

    /**
     * 创建作品（支持自动创建艺术家）
     * 如果填入的艺术家不在数据库中，会自动创建新艺术家
     */
    @PostMapping("/artworks")
    public Result<Long> createArtwork(@RequestBody Map<String, Object> params) {
        Long artworkId = productAdminPersistenceService.createArtwork(params);
        return Result.success(artworkId);
    }

    /**
     * 作品列表 - /admin/product/list
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String id,
        @RequestParam(required = false) String artworkCode,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String authorName,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String artType,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(productAdminPersistenceService.listProducts(
            keyword, id, artworkCode, title, authorName, categoryId, artType, status, page, size
        ));
    }
}
