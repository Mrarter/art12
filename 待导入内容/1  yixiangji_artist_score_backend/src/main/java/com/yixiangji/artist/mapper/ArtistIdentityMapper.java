package com.yixiangji.artist.mapper;

import com.yixiangji.artist.entity.ArtistIdentity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArtistIdentityMapper {
    ArtistIdentity findByArtistId(Long artistId);
}
