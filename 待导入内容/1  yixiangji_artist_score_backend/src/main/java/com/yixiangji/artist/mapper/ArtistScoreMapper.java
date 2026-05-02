package com.yixiangji.artist.mapper;

import com.yixiangji.artist.entity.ArtistScore;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArtistScoreMapper {
    ArtistScore findByArtistId(Long artistId);
    void upsert(ArtistScore score);
}
