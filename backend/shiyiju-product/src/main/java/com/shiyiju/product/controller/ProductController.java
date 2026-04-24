package com.shiyiju.product.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.product.dto.ArtworkQueryDTO;
import com.shiyiju.product.dto.ArtworkUpdateDTO;
import com.shiyiju.product.dto.BatchStatusDTO;
import com.shiyiju.product.entity.Banner;
import com.shiyiju.product.entity.Category;
import com.shiyiju.product.service.ProductService;
import com.shiyiju.common.vo.ArtworkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Value("${upload.local.path:/tmp/shiyiju-uploads}")
    private String localPath;

    @Value("${upload.cdn-url:http://localhost:8087}")
    private String cdnUrl;

    /** 上传作品图片 (POST /product/upload) */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.fail(400, "文件大小不能超过 10MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(400, "只支持图片文件");
        }

        try {
            // 本地存储方式
            String datePath = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String extension = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            String relativePath = "images/" + datePath + "/" + fileName;
            
            // 确保目录存在
            java.nio.file.Path dirPath = java.nio.file.Paths.get(localPath, "images", datePath);
            java.nio.file.Files.createDirectories(dirPath);
            
            // 保存文件
            java.nio.file.Path filePath = java.nio.file.Paths.get(localPath, relativePath);
            file.transferTo(filePath.toFile());
            
            String fileUrl = cdnUrl + "/upload/" + relativePath;
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", file.getOriginalFilename());
            log.info("文件已上传: {}", fileUrl);
            return Result.success(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.fail(500, "文件上传失败: " + e.getMessage());
        }
    }
    
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 获取作品列表 (GET /product/list)
     * 支持分类、筛选、分页
     */
    @GetMapping("/list")
    public Result<PageResult<ArtworkVO>> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer holdDuration,
            @RequestParam(required = false) Integer status,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        ArtworkQueryDTO query = new ArtworkQueryDTO();
        query.setPage(page);
        query.setPageSize(size != null ? size : pageSize);
        query.setId(id);
        query.setTitle(title);
        query.setAuthorName(authorName);
        query.setCategoryId(categoryId);
        query.setSort(sort);
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);
        query.setYearFrom(yearFrom);
        query.setYearTo(yearTo);
        query.setRegion(region);
        query.setHoldDuration(holdDuration);
        query.setStatus(status);
        return Result.success(productService.getProductList(query, userId));
    }

    /**
     * 获取作品详情 (GET /product/{id})
     */
    @GetMapping("/{id}")
    public Result<ArtworkVO> getProductDetail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return Result.success(productService.getArtworkDetail(id, userId));
    }

    /**
     * 搜索作品 (GET /product/search)
     */
    @GetMapping("/search")
    public Result<PageResult<ArtworkVO>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        ArtworkQueryDTO query = new ArtworkQueryDTO();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setKeyword(keyword);
        return Result.success(productService.getProductList(query, userId));
    }

    /**
     * 获取Banner列表 (GET /product/banners)
     */
    @GetMapping("/banners")
    public Result<List<Banner>> getBanners() {
        return Result.success(productService.getBanners());
    }

    /**
     * 获取分类列表 (GET /product/categories)
     */
    @GetMapping("/categories")
    public Result<List<Category>> getCategoryList() {
        return Result.success(productService.getCategoryList());
    }

    /**
     * 收藏作品 (POST /product/favorite)
     */
    @PostMapping("/favorite")
    public Result<Void> favoriteProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Long artworkId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        productService.favoriteArtwork(artworkId, userId);
        return Result.success();
    }

    /**
     * 取消收藏 (DELETE /product/favorite/{id})
     */
    @DeleteMapping("/favorite/{id}")
    public Result<Void> unfavoriteProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        productService.unfavoriteArtwork(id, userId);
        return Result.success();
    }

    /**
     * 我的收藏列表 (GET /product/favorites)
     */
    @GetMapping("/favorites")
    public Result<PageResult<ArtworkVO>> getMyFavorites(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(productService.getMyFavorites(userId, page, pageSize));
    }

    // ==================== 兼容旧路径的接口 ====================

    /** 获取作品列表 (旧路径) */
    @GetMapping("/artwork/list")
    public Result<PageResult<ArtworkVO>> getArtworkList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        ArtworkQueryDTO query = new ArtworkQueryDTO();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setCategoryId(categoryId);
        query.setKeyword(keyword);
        query.setSortBy(sortBy);
        query.setSortOrder(sortOrder);
        return Result.success(productService.getProductList(query, userId));
    }

    /** 获取作品详情 (旧路径) */
    @GetMapping("/artwork/detail/{id}")
    public Result<ArtworkVO> getArtworkDetail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return Result.success(productService.getArtworkDetail(id, userId));
    }

    /** 获取首页Banner (旧路径) */
    @GetMapping("/homepage/banners")
    public Result<List<Banner>> getBannersLegacy() {
        return Result.success(productService.getBanners());
    }

    /** 收藏作品 (旧路径) */
    @PostMapping("/artwork/favorite/{id}")
    public Result<Void> favoriteArtwork(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId
    ) {
        productService.favoriteArtwork(id, userId);
        return Result.success();
    }

    /** 取消收藏 (旧路径) */
    @PostMapping("/artwork/unfavorite/{id}")
    public Result<Void> unfavoriteArtwork(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId
    ) {
        productService.unfavoriteArtwork(id, userId);
        return Result.success();
    }

    /**
     * 更新作品 (PUT /product/update)
     * 用于管理后台编辑作品
     */
    @PutMapping("/update")
    public Result<Void> updateProduct(@RequestBody ArtworkUpdateDTO updateDTO) {
        productService.updateArtwork(updateDTO);
        return Result.success();
    }

    /**
     * 批量更新作品状态 ( PUT /product/batch/status)
     */
    @PutMapping("/batch/status")
    public Result<Void> batchUpdateStatus(@RequestBody BatchStatusDTO dto) {
        productService.batchUpdateStatus(dto.getIds(), dto.getStatus());
        return Result.success();
    }

    /**
     * 创建作品 (POST /product/create)
     */
    @PostMapping("/create")
    public Result<Long> createProduct(@RequestBody ArtworkUpdateDTO dto) {
        Long id = productService.createArtwork(dto);
        return Result.success(id);
    }

/**
 * 删除作品 (DELETE /product/{id})
 */
@DeleteMapping("/{id}")
public Result<Void> deleteProduct(@PathVariable Long id) {
  productService.deleteArtwork(id);
  return Result.success();
}

/**
 * 获取推荐作品 (GET /product/recommend)
 * 复用作品列表接口
 */
@GetMapping("/recommend")
public Result<PageResult<ArtworkVO>> getRecommend(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestHeader(value = "X-User-Id", required = false) Long userId
) {
  ArtworkQueryDTO query = new ArtworkQueryDTO();
  query.setPage(page);
  query.setPageSize(pageSize);
  return Result.success(productService.getProductList(query, userId));
}

/**
 * 获取关注艺术家作品 (GET /product/following)
 * 复用作品列表接口
 */
@GetMapping("/following")
public Result<PageResult<ArtworkVO>> getFollowingWorks(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestHeader(value = "X-User-Id", required = false) Long userId
) {
  ArtworkQueryDTO query = new ArtworkQueryDTO();
  query.setPage(page);
  query.setPageSize(pageSize);
  return Result.success(productService.getProductList(query, userId));
}
}
