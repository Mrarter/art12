package com.shiyiju.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.admin.entity.AuctionLot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 拍卖拍品Mapper
 */
@Mapper
public interface AuctionLotMapper extends BaseMapper<AuctionLot> {

    @Select("SELECT COUNT(*) FROM auction_lot WHERE session_id = #{sessionId}")
    int countBySessionId(Long sessionId);
}
