package com.shiyiju.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.admin.entity.CommissionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 佣金记录Mapper
 */
@Mapper
public interface CommissionLogMapper extends BaseMapper<CommissionLog> {

    @Select("SELECT DATE(create_time) as date, SUM(commission_amount) as amount FROM commission_log " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY) GROUP BY DATE(create_time)")
    List<Map<String, Object>> getCommissionTrend(int days);

    @Select("SELECT SUM(commission_amount) FROM commission_log WHERE status = 1")
    BigDecimal getTotalPaidCommission();

    @Select("SELECT SUM(commission_amount) FROM commission_log WHERE status = 0")
    BigDecimal getTotalPendingCommission();
}
