package com.shiyiju.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.admin.entity.AuctionBid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 拍卖出价记录Mapper
 */
@Mapper
public interface AuctionBidMapper extends BaseMapper<AuctionBid> {

    @Select("SELECT COUNT(*) FROM auction_bid WHERE lot_id = #{lotId}")
    int countByLotId(Long lotId);

    @Select("SELECT SUM(bid_price) FROM auction_bid WHERE lot_id = #{lotId} AND status = 1")
    java.math.BigDecimal getMaxBidPrice(Long lotId);
}
