package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.CommissionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一佣金记录 Mapper
 */
@Mapper
public interface CommissionRecordMapper extends BaseMapper<CommissionRecord> {
}
