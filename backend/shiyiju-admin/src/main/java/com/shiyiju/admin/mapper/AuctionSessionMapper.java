package com.shiyiju.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.admin.entity.AuctionSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 拍卖专场Mapper
 */
@Mapper
public interface AuctionSessionMapper extends BaseMapper<AuctionSession> {

    @Select("SELECT COUNT(*) FROM auction_session WHERE status = #{status}")
    int countByStatus(Integer status);
}
