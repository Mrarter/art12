package com.shiyiju.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.product.entity.ArtistIdentity;
import com.shiyiju.product.entity.ArtistScore;
import com.shiyiju.product.entity.ArtistScoreAdjustLog;
import com.shiyiju.product.mapper.ArtistIdentityMapper;
import com.shiyiju.product.mapper.ArtistScoreAdjustLogMapper;
import com.shiyiju.product.mapper.ArtistScoreMapper;
import com.shiyiju.product.service.ArtistScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArtistScoreServiceImpl implements ArtistScoreService {

    private final ArtistScoreMapper artistScoreMapper;
    private final ArtistIdentityMapper artistIdentityMapper;
    private final ArtistScoreAdjustLogMapper adjustLogMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public ArtistScore getScore(Long artistId) {
        LambdaQueryWrapper<ArtistScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArtistScore::getArtistId, artistId);
        return artistScoreMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public ArtistScore recalculateScore(Long artistId) {
        LambdaQueryWrapper<ArtistIdentity> idWrapper = new LambdaQueryWrapper<>();
        idWrapper.eq(ArtistIdentity::getArtistId, artistId);
        ArtistIdentity identity = artistIdentityMapper.selectOne(idWrapper);

        // 各维度分值（目前仅学术和互联网有值）
        int academicScore = calculateAcademicScore(identity);
        int internetScore = calculateInternetScore(identity);
        int salesScore = 0;
        int influenceScore = 0;
        int activityScore = 0;
        int qualityScore = calculateQualityScore(artistId);
        int reviewScore = 0;

        ArtistScore score = getScore(artistId);
        boolean isNew = (score == null);
        if (isNew) {
            score = new ArtistScore();
            score.setArtistId(artistId);
        }

        score.setSalesScore(salesScore);
        score.setInfluenceScore(influenceScore);
        score.setActivityScore(activityScore);
        score.setQualityScore(qualityScore);
        score.setReviewScore(reviewScore);
        score.setAcademicScore(academicScore);
        score.setInternetScore(internetScore);

        // 保留现有手动调分值
        int adjustmentScoreVal = (!isNew && score.getAdjustmentScore() != null) ? score.getAdjustmentScore() : 0;
        score.setAdjustmentScore(adjustmentScoreVal);

        int totalScore = salesScore + influenceScore + activityScore + qualityScore
                + reviewScore + academicScore + internetScore + adjustmentScoreVal;

        score.setTotalScore(totalScore);
        boolean isVerified = identity != null && identity.getVerified() != null && identity.getVerified() == 1;
        score.setLevel(calculateLevel(totalScore, isVerified));
        score.setUpdatedAt(LocalDateTime.now());

        if (isNew) {
            score.setCreatedAt(LocalDateTime.now());
            artistScoreMapper.insert(score);
        } else {
            artistScoreMapper.updateById(score);
        }

        return score;
    }

    @Override
    @Transactional
    public ArtistScore manualAdjust(Long artistId, int adjustScore, String reason, Long operatorId) {
        ArtistScore score = getScore(artistId);
        if (score == null) {
            score = new ArtistScore();
            score.setArtistId(artistId);
            score.setTotalScore(0);
            // 新建时，后续会通过calculateLevel重新计算
            score.setLevel("B");
            score.setCreatedAt(LocalDateTime.now());
        }

        int oldScore = score.getTotalScore() != null ? score.getTotalScore() : 0;
        int newScore = Math.max(0, Math.min(1000, oldScore + adjustScore));

        // 累积手动调分到 adjustmentScore
        int oldAdjust = score.getAdjustmentScore() != null ? score.getAdjustmentScore() : 0;
        score.setAdjustmentScore(oldAdjust + adjustScore);
        score.setTotalScore(newScore);
        score.setLevel(calculateLevel(newScore, true));
        score.setUpdatedAt(LocalDateTime.now());
        artistScoreMapper.updateById(score);

        ArtistScoreAdjustLog log = new ArtistScoreAdjustLog();
        log.setArtistId(artistId);
        log.setOldScore(oldScore);
        log.setAdjustScore(adjustScore);
        log.setNewScore(newScore);
        log.setReason(reason);
        log.setOperatorId(operatorId);
        log.setCreatedAt(LocalDateTime.now());
        adjustLogMapper.insert(log);

        return score;
    }

    /**
     * 计算作品信息完整度分（基于作品信息完整度，满分50）
     * - 封面图: 8分
     * - 作品描述: 10分
     * - 尺寸信息: 6分
     * - 创作年份: 5分
     * - 材质/类型: 5分
     * - 多图展示: 6分
     * - 定价: 4分
     * - 分类: 3分
     * - 评分(rating): 3分
     * 每件作品满分50，取所有作品平均分
     */
    private int calculateQualityScore(Long artistId) {
        try {
            List<Map<String, Object>> artworks = jdbcTemplate.queryForList(
                "SELECT cover, cover_image, images, description, size, `year`, medium, art_type, price, category_id, rating FROM artwork WHERE author_id = ?",
                artistId);
            if (artworks.isEmpty()) return 0;

            int total = 0;
            for (Map<String, Object> aw : artworks) {
                int score = 0;
                boolean hasCover = aw.get("cover") != null && !((String) aw.get("cover")).isEmpty();
                boolean hasCoverImage = aw.get("cover_image") != null && !((String) aw.get("cover_image")).isEmpty();
                if (hasCover || hasCoverImage) score += 8;

                String desc = (String) aw.get("description");
                if (desc != null && !desc.isEmpty()) score += 10;

                String size = (String) aw.get("size");
                if (size != null && !size.isEmpty()) score += 6;

                Integer year = (Integer) aw.get("year");
                if (year != null && year > 0) score += 5;

                String medium = (String) aw.get("medium");
                String artType = (String) aw.get("art_type");
                if ((medium != null && !medium.isEmpty()) || (artType != null && !artType.isEmpty())) score += 5;

                String images = (String) aw.get("images");
                if (images != null && !images.isEmpty() && !images.equals("[]")) score += 6;

                java.math.BigDecimal price = (java.math.BigDecimal) aw.get("price");
                if (price != null && price.compareTo(java.math.BigDecimal.ZERO) > 0) score += 4;

                Integer categoryId = (Integer) aw.get("category_id");
                if (categoryId != null && categoryId > 0) score += 3;

                java.math.BigDecimal rating = (java.math.BigDecimal) aw.get("rating");
                if (rating != null && rating.compareTo(java.math.BigDecimal.ZERO) > 0) score += 3;

                total += Math.min(score, 50);
            }
            return Math.min(total / artworks.size(), 50);
        } catch (Exception e) {
            return 0;
        }
    }

    private int calculateAcademicScore(ArtistIdentity identity) {
        if (identity == null || identity.getVerified() == null || identity.getVerified() != 1) return 0;
        int score = 0;
        String school = identity.getSchoolName();
        String title = identity.getAcademicTitle();
        String association = identity.getAssociationName();

        if (school != null) {
            if (school.contains("中央美术学院") || school.contains("中国美术学院") || school.contains("清华大学美术学院")) {
                score += 40;
            } else if (school.contains("美术学院")) {
                score += 30;
            } else {
                score += 15;
            }
        }
        if ("教授".equals(title)) score += 40;
        else if ("副教授".equals(title)) score += 30;
        else if ("讲师".equals(title)) score += 20;
        if (association != null && association.contains("美协")) score += 20;
        return Math.min(score, 200);
    }

    private int calculateInternetScore(ArtistIdentity identity) {
        if (identity == null || identity.getVerified() == null || identity.getVerified() != 1) return 0;
        int score = 0;
        int followers = identity.getFollowerCount() == null ? 0 : identity.getFollowerCount();
        if (followers >= 1000000) score += 25;
        else if (followers >= 500000) score += 20;
        else if (followers >= 100000) score += 15;
        else if (followers >= 50000) score += 10;
        else if (followers >= 10000) score += 5;
        score += Math.min(identity.getContentQualityScore() == null ? 0 : identity.getContentQualityScore(), 15);
        score += Math.min(identity.getConversionScore() == null ? 0 : identity.getConversionScore(), 10);
        return Math.min(score, 50);
    }

    /**
     * 计算艺术家等级
     * 规则：
     * - 未认证（无artist_identity记录）: U（前端显示"未认证"）
     * - ≥900: S+
     * - ≥750: S
     * - ≥500: A+
     * - ≥250: A
     * - <250: B
     */
    private String calculateLevel(int totalScore, boolean isVerified) {
        if (!isVerified) {
            return "U";
        }
        if (totalScore >= 900) return "S+";
        if (totalScore >= 750) return "S";
        if (totalScore >= 500) return "A+";
        if (totalScore >= 250) return "A";
        return "B";
    }
}
