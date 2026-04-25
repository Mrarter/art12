package com.shiyiju.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.product.entity.CommissionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 佣金记录Mapper
 */
@Mapper
public interface CommissionLogMapper extends BaseMapper<CommissionLog> {
}
