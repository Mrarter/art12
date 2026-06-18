package com.shiyiju.product.controller.admin;

import com.shiyiju.common.result.Result;
import com.shiyiju.product.entity.Artwork;
import com.shiyiju.product.entity.ArtworkPriceLog;
import com.shiyiju.product.mapper.ArtworkMapper;
import com.shiyiju.product.mapper.ArtworkPriceLogMapper;
import com.shiyiju.product.service.ArtworkPriceService;
import com.shiyiju.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/artwork/price")
@RequiredArgsConstructor
public class ArtworkPriceAdminController {

    private final ArtworkMapper artworkMapper;
    private final ArtworkPriceLogMapper priceLogMapper;
    private final ArtworkPriceService artworkPriceService;
    private final ProductService productService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listPrices(
            @RequestParam(required = false) String keyword) {
        List<Artwork> artworks = artworkMapper.selectList(null);
        List<Map<String, Object>> result = artworks.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("artworkId", a.getId());
            m.put("title", a.getTitle());
            m.put("artistName", a.getAuthorName());
            m.put("currentPrice", productService.calculateDisplayCurrentPrice(a.getId()));
            m.put("collectCount", a.getFavoriteCount());
            m.put("saleCount", a.getSaleCount());
            m.put("status", a.getStatus());
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @PostMapping("/manual-adjust")
    public Result<Long> manualAdjust(@RequestBody Map<String, Object> params) {
        ensurePriceLogTable();
        Long artworkId = Long.valueOf(params.get("artworkId").toString());
        Long newPrice = Long.valueOf(params.get("newPrice").toString());
        String reason = (String) params.get("reason");
        return Result.success(artworkPriceService.manualAdjust(artworkId, newPrice, reason, 0L));
    }

    @GetMapping("/logs")
    public Result<List<ArtworkPriceLog>> logs(
            @RequestParam(required = false) Long artworkId,
            @RequestParam(required = false) String changeReason) {
        ensurePriceLogTable();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArtworkPriceLog> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (artworkId != null) wrapper.eq(ArtworkPriceLog::getArtworkId, artworkId);
        if (changeReason != null && !changeReason.isEmpty()) wrapper.eq(ArtworkPriceLog::getChangeReason, changeReason);
        wrapper.orderByDesc(ArtworkPriceLog::getCreatedAt);
        List<ArtworkPriceLog> logs = priceLogMapper.selectList(wrapper);
        return Result.success(logs);
    }

    private void ensurePriceLogTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS artwork_price_log (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                artwork_id BIGINT NOT NULL,
                artist_id BIGINT NULL,
                old_price BIGINT NULL,
                new_price BIGINT NULL,
                change_rate DECIMAL(12, 6) NULL,
                change_reason VARCHAR(32) NULL,
                remark VARCHAR(255) NULL,
                operator_id BIGINT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_artwork_price_log_artwork_id (artwork_id),
                INDEX idx_artwork_price_log_created_at (created_at)
            )
            """);
    }
}
