package com.shiyiju.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.admin.entity.PromoterRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 艺荐官Mapper
 */
@Mapper
public interface PromoterRecordMapper extends BaseMapper<PromoterRecord> {

    @Select("SELECT level, COUNT(*) as count FROM promoter_record GROUP BY level")
    List<Map<String, Object>> countByLevel();

    @Select("SELECT SUM(total_sales) FROM promoter_record")
    BigDecimal getTotalSales();

    @Select("SELECT COUNT(*) FROM promoter_record WHERE level = #{level}")
    int countByLevel(Integer level);
}
