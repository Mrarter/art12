package com.shiyiju.auction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.auction.entity.AuctionDeposit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuctionDepositMapper extends BaseMapper<AuctionDeposit> {
    @Select("SELECT * FROM auction_deposit WHERE session_id = #{sessionId} AND user_id = #{userId} LIMIT 1 FOR UPDATE")
    AuctionDeposit selectSessionDepositForUpdate(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
