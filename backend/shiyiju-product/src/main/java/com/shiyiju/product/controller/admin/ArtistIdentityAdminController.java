package com.shiyiju.product.controller.admin;

import com.shiyiju.common.result.Result;
import com.shiyiju.product.entity.ArtistIdentity;
import com.shiyiju.product.mapper.ArtistIdentityMapper;
import com.shiyiju.product.service.ArtistScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/artist/identity")
@RequiredArgsConstructor
public class ArtistIdentityAdminController {

    private final ArtistIdentityMapper artistIdentityMapper;
    private final ArtistScoreService artistScoreService;
    private final JdbcTemplate jdbcTemplate;

    private String getArtistName(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COALESCE(real_name, artist_name, '') FROM artist_profile WHERE user_id = ?",
                String.class, userId);
        } catch (Exception e) {
            return "";
        }
    }

    @GetMapping("/audit-list")
    public Result<List<Map<String, Object>>> auditList(
            @RequestParam(required = false) String auditStatus) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArtistIdentity> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (auditStatus != null && !auditStatus.isEmpty()) {
            wrapper.eq(ArtistIdentity::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(ArtistIdentity::getCreatedAt);
        List<ArtistIdentity> list = artistIdentityMapper.selectList(wrapper);
        List<Map<String, Object>> result = list.stream().map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("artistId", i.getArtistId());
            m.put("artistName", getArtistName(i.getArtistId()));
            m.put("schoolName", i.getSchoolName());
            m.put("degree", i.getDegree());
            m.put("academicTitle", i.getAcademicTitle());
            m.put("socialPlatform", i.getSocialPlatform());
            m.put("followerCount", i.getFollowerCount());
            m.put("auditStatus", i.getAuditStatus());
            m.put("auditRemark", i.getAuditRemark());
            m.put("socialAccountUrl", i.getSocialAccountUrl());
            m.put("exhibitions", i.getExhibitions());
            m.put("awards", i.getAwards());
            m.put("associationName", i.getAssociationName());
            m.put("contentType", i.getContentType());
            m.put("contentQualityScore", i.getContentQualityScore());
            m.put("conversionScore", i.getConversionScore());
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/{artistId}")
    public Result<ArtistIdentity> getIdentity(@PathVariable Long artistId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArtistIdentity> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(ArtistIdentity::getArtistId, artistId);
        ArtistIdentity identity = artistIdentityMapper.selectOne(wrapper);
        return Result.success(identity);
    }

    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody Map<String, Object> params) {
        Long artistId = Long.valueOf(params.get("artistId").toString());
        String auditStatus = (String) params.get("auditStatus");
        String auditRemark = (String) params.get("auditRemark");

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArtistIdentity> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(ArtistIdentity::getArtistId, artistId);
        ArtistIdentity identity = artistIdentityMapper.selectOne(wrapper);
        if (identity == null) {
            return Result.fail("资质信息不存在");
        }

        identity.setAuditStatus(auditStatus);
        identity.setVerified("PASS".equals(auditStatus) ? 1 : 0);
        identity.setAuditRemark(auditRemark);
        identity.setUpdatedAt(LocalDateTime.now());
        artistIdentityMapper.updateById(identity);

        if ("PASS".equals(auditStatus)) {
            artistScoreService.recalculateScore(artistId);
        }

        return Result.success();
    }
}
