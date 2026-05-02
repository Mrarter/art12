package com.yixiangji.artwork.mapper;

import com.yixiangji.artwork.entity.ArtworkPriceLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArtworkPriceLogMapper {
    void insert(ArtworkPriceLog log);
}
