package com.shiyiju.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.product.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款/售后记录Mapper
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {
}
