package com.shiyiju.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作品表Mapper（复用业务表）
 */
@Mapper
public interface ArtworkMapper extends BaseMapper<com.shiyiju.admin.entity.Artwork> {
}
