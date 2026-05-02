package com.yixiangji.artist.service.impl;

import com.yixiangji.artist.entity.ArtistIdentity;
import com.yixiangji.artist.entity.ArtistScore;
import com.yixiangji.artist.mapper.ArtistIdentityMapper;
import com.yixiangji.artist.mapper.ArtistScoreMapper;
import com.yixiangji.artist.service.ArtistScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistScoreServiceImpl implements ArtistScoreService {

    private final ArtistScoreMapper artistScoreMapper;
    private final ArtistIdentityMapper artistIdentityMapper;

    @Override
    public ArtistScore recalculateScore(Long artistId) {
        ArtistIdentity identity = artistIdentityMapper.findByArtistId(artistId);
        int academicScore = calculateAcademicScore(identity);
        int internetScore = calculateInternetScore(identity);

        int salesScore = 0;
        int influenceScore = 0;
        int activityScore = 0;
        int qualityScore = 0;
        int reviewScore = 0;

        int totalScore = salesScore + influenceScore + activityScore + qualityScore + reviewScore + academicScore + internetScore;

        ArtistScore score = new ArtistScore();
        score.setArtistId(artistId);
        score.setSalesScore(salesScore);
        score.setInfluenceScore(influenceScore);
        score.setActivityScore(activityScore);
        score.setQualityScore(qualityScore);
        score.setReviewScore(reviewScore);
        score.setAcademicScore(academicScore);
        score.setInternetScore(internetScore);
        score.setTotalScore(totalScore);
        score.setLevel(calculateLevel(totalScore));

        artistScoreMapper.upsert(score);
        return score;
    }

    @Override
    public ArtistScore getScore(Long artistId) {
        return artistScoreMapper.findByArtistId(artistId);
    }

    private int calculateAcademicScore(ArtistIdentity identity) {
        if (identity == null || identity.getVerified() == null || identity.getVerified() != 1) return 0;
        int score = 0;
        String school = identity.getSchoolName();
        String degree = identity.getDegree();
        String title = identity.getAcademicTitle();
        String association = identity.getAssociationName();

        if (school != null) {
            if (school.contains("中央美术学院") || school.contains("中国美术学院") || school.contains("清华大学美术学院")) score += 40;
            else if (school.contains("美术学院")) score += 30;
            else score += 15;
        }
        if ("硕士".equals(degree) || "博士".equals(degree)) score += 10;
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
