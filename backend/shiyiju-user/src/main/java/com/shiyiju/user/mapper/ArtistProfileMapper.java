package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.ArtistProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 艺术家档案 Mapper
 */
@Mapper
public interface ArtistProfileMapper extends BaseMapper<ArtistProfile> {

    /**
     * 根据用户ID查询艺术家档案
     * 支持多种表结构（user_id, userUid, uid 等列名）
     */
    @Select("SELECT * FROM artist_profile WHERE " +
           "user_id = #{userId} OR user_uid = #{userId} OR uid = #{userId} " +
           "LIMIT 1")
    ArtistProfile selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户UID查询艺术家档案
     */
    @Select("SELECT * FROM artist_profile WHERE user_uid = #{userUid} LIMIT 1")
    ArtistProfile selectByUserUid(@Param("userUid") String userUid);

    /**
     * 根据艺术家编号查询
     */
    @Select("SELECT * FROM artist_profile WHERE artist_code = #{artistCode} LIMIT 1")
    ArtistProfile selectByArtistCode(@Param("artistCode") String artistCode);
}
