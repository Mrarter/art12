package com.shiyiju.product.service;

import com.shiyiju.product.entity.ArtistScore;

public interface ArtistScoreService {
    ArtistScore getScore(Long artistId);
    ArtistScore recalculateScore(Long artistId);
    ArtistScore manualAdjust(Long artistId, int adjustScore, String reason, Long operatorId);
}
