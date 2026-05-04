package com.shiyiju.product.controller.admin;

import com.shiyiju.common.result.Result;
import com.shiyiju.product.entity.Artwork;
import com.shiyiju.product.entity.ArtworkPriceLog;
import com.shiyiju.product.mapper.ArtworkMapper;
import com.shiyiju.product.mapper.ArtworkPriceLogMapper;
import com.shiyiju.product.service.ArtworkPriceService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listPrices(
            @RequestParam(required = false) String keyword) {
        List<Artwork> artworks = artworkMapper.selectList(null);
        List<Map<String, Object>> result = artworks.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("artworkId", a.getId());
            m.put("title", a.getTitle());
            m.put("artistName", a.getAuthorName());
            m.put("currentPrice", a.getPrice());
            m.put("collectCount", a.getFavoriteCount());
            m.put("saleCount", a.getSaleCount());
            m.put("status", a.getStatus());
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @PostMapping("/manual-adjust")
    public Result<Long> manualAdjust(@RequestBody Map<String, Object> params) {
        Long artworkId = Long.valueOf(params.get("artworkId").toString());
        Long newPrice = Long.valueOf(params.get("newPrice").toString());
        String reason = (String) params.get("reason");
        return Result.success(artworkPriceService.manualAdjust(artworkId, newPrice, reason, 0L));
    }

    @GetMapping("/logs")
    public Result<List<ArtworkPriceLog>> logs(
            @RequestParam(required = false) Long artworkId,
            @RequestParam(required = false) String changeReason) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArtworkPriceLog> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (artworkId != null) wrapper.eq(ArtworkPriceLog::getArtworkId, artworkId);
        if (changeReason != null && !changeReason.isEmpty()) wrapper.eq(ArtworkPriceLog::getChangeReason, changeReason);
        wrapper.orderByDesc(ArtworkPriceLog::getCreatedAt);
        List<ArtworkPriceLog> logs = priceLogMapper.selectList(wrapper);
        return Result.success(logs);
    }
}
