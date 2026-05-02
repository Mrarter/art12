package com.yixiangji.artist.service;

import com.yixiangji.artist.entity.ArtistScore;

public interface ArtistScoreService {
    ArtistScore recalculateScore(Long artistId);
    ArtistScore getScore(Long artistId);
}
