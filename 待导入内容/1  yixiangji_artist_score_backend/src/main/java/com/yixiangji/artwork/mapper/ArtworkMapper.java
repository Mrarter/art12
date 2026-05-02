package com.yixiangji.artwork.mapper;

import com.yixiangji.artwork.entity.Artwork;
import org.apache.ibatis.annotations.Mapper;
import java.math.BigDecimal;

@Mapper
public interface ArtworkMapper {
    Artwork findById(Long id);
    void updateCurrentPrice(Long id, BigDecimal currentPrice);
}
