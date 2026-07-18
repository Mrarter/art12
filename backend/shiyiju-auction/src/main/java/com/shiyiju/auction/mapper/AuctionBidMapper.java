package com.shiyiju.auction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.auction.entity.AuctionBid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuctionBidMapper extends BaseMapper<AuctionBid> {
    @Update("UPDATE auction_bid SET status = 0 WHERE lot_id = #{lotId} AND status = 1")
    int clearLeadingBid(@Param("lotId") Long lotId);
}
