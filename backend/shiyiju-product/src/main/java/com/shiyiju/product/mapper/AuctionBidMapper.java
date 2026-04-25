package com.shiyiju.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.product.entity.AuctionBid;
import org.apache.ibatis.annotations.Mapper;

/**
 * 拍卖出价记录Mapper
 */
@Mapper
public interface AuctionBidMapper extends BaseMapper<AuctionBid> {
}
