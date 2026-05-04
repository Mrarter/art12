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

@Service
@RequiredArgsConstructor
public class ArtistScoreServiceImpl implements ArtistScoreService {

    private final ArtistScoreMapper artistScoreMapper;
    private final ArtistIdentityMapper artistIdentityMapper;
    private final ArtistScoreAdjustLogMapper adjustLogMapper;

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

        int academicScore = calculateAcademicScore(identity);
        int internetScore = calculateInternetScore(identity);
        int salesScore = 0;
        int influenceScore = 0;
        int activityScore = 0;
        int qualityScore = 0;
        int reviewScore = 0;

        int totalScore = salesScore + influenceScore + activityScore + qualityScore
                + reviewScore + academicScore + internetScore;

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
        score.setTotalScore(totalScore);
        score.setLevel(calculateLevel(totalScore));
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
            score.setLevel("D");
            score.setCreatedAt(LocalDateTime.now());
        }

        int oldScore = score.getTotalScore() != null ? score.getTotalScore() : 0;
        int newScore = Math.max(0, Math.min(1000, oldScore + adjustScore));

        score.setTotalScore(newScore);
        score.setLevel(calculateLevel(newScore));
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
        return Math.min(score, 100);
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

    private String calculateLevel(int totalScore) {
        if (totalScore >= 900) return "A+";
        if (totalScore >= 750) return "A";
        if (totalScore >= 500) return "B";
        if (totalScore >= 250) return "C";
        return "D";
    }
}
