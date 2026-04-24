package com.shiyiju.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.admin.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    @Select("SELECT COUNT(*) FROM order_info WHERE status = #{status}")
    int countByStatus(Integer status);

    @Select("SELECT SUM(pay_amount) FROM order_info WHERE status = 2")
    java.math.BigDecimal getTotalSales();
}
