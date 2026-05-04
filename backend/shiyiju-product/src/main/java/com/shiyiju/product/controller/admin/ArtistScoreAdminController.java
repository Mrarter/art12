package com.shiyiju.product.controller.admin;

import com.shiyiju.common.result.Result;
import com.shiyiju.product.entity.ArtistScore;
import com.shiyiju.product.entity.ArtistScoreAdjustLog;
import com.shiyiju.product.mapper.ArtistScoreAdjustLogMapper;
import com.shiyiju.product.mapper.ArtistScoreMapper;
import com.shiyiju.product.service.ArtistScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/artist/score")
@RequiredArgsConstructor
public class ArtistScoreAdminController {

    private final ArtistScoreService artistScoreService;
    private final ArtistScoreMapper artistScoreMapper;
    private final ArtistScoreAdjustLogMapper adjustLogMapper;
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

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listScores(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level) {
        List<ArtistScore> list = artistScoreMapper.selectList(null);
        List<Map<String, Object>> result = list.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("artistId", s.getArtistId());
            m.put("artistName", getArtistName(s.getArtistId()));
            m.put("totalScore", s.getTotalScore());
            m.put("level", s.getLevel());
            m.put("academicScore", s.getAcademicScore());
            m.put("internetScore", s.getInternetScore());
            m.put("updatedAt", s.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @PostMapping("/manual-adjust")
    public Result<ArtistScore> manualAdjust(@RequestBody Map<String, Object> params) {
        Long artistId = Long.valueOf(params.get("artistId").toString());
        int adjustScore = Integer.parseInt(params.get("adjustScore").toString());
        String reason = (String) params.get("reason");
        ArtistScore score = artistScoreService.manualAdjust(artistId, adjustScore, reason, 0L);
        return Result.success(score);
    }

    @GetMapping("/adjust-logs")
    public Result<List<ArtistScoreAdjustLog>> adjustLogs(@RequestParam(required = false) Long artistId) {
        List<ArtistScoreAdjustLog> logs;
        if (artistId != null) {
            logs = adjustLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArtistScoreAdjustLog>()
                    .eq(ArtistScoreAdjustLog::getArtistId, artistId)
                    .orderByDesc(ArtistScoreAdjustLog::getCreatedAt));
        } else {
            logs = adjustLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArtistScoreAdjustLog>()
                    .orderByDesc(ArtistScoreAdjustLog::getCreatedAt));
        }
        return Result.success(logs);
    }
}
