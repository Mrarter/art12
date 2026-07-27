package com.shiyiju.auction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.auction.entity.AuctionLot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuctionLotMapper extends BaseMapper<AuctionLot> {
    @Select("SELECT * FROM auction_lot WHERE id = #{id} FOR UPDATE")
    AuctionLot selectByIdForUpdate(Long id);
}
