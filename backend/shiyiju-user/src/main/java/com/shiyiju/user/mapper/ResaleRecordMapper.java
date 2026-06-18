package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.ResaleRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 转售记录 Mapper
 * 所有写操作均带乐观锁（version）校验
 */
@Mapper
public interface ResaleRecordMapper extends BaseMapper<ResaleRecord> {

    /**
     * 更新转售状态（pending -> paid）
     */
    @Update("UPDATE resale_record SET status = #{newStatus}, buyer_user_id = #{buyerUserId}, " +
            "trade_no = #{tradeNo}, artist_income = #{artistIncome}, platform_fee = #{platformFee}, " +
            "seller_income = #{sellerIncome}, version = version + 1, updated_time = NOW() " +
            "WHERE id = #{id} AND status = #{oldStatus} AND version = #{version}")
    int updateStatus(@Param("id") Long id,
                     @Param("oldStatus") String oldStatus,
                     @Param("newStatus") String newStatus,
                     @Param("buyerUserId") Long buyerUserId,
                     @Param("tradeNo") String tradeNo,
                     @Param("artistIncome") BigDecimal artistIncome,
                     @Param("platformFee") BigDecimal platformFee,
                     @Param("sellerIncome") BigDecimal sellerIncome,
                     @Param("version") Integer version);

    /**
     * 完成转售（paid -> completed，填充结算数据）
     */
    @Update("UPDATE resale_record SET status = 'completed', buyer_user_id = #{buyerUserId}, " +
            "artist_income = #{artistIncome}, platform_fee = #{platformFee}, seller_income = #{sellerIncome}, " +
            "version = version + 1, updated_time = NOW() " +
            "WHERE id = #{id} AND status = 'paid' AND version = #{version}")
    int completeResale(@Param("id") Long id,
                       @Param("buyerUserId") Long buyerUserId,
                       @Param("artistIncome") BigDecimal artistIncome,
                       @Param("platformFee") BigDecimal platformFee,
                       @Param("sellerIncome") BigDecimal sellerIncome,
                       @Param("version") Integer version);

    /**
     * 取消转售（pending -> cancel）
     */
    @Update("UPDATE resale_record SET status = 'cancel', version = version + 1, updated_time = NOW() " +
            "WHERE id = #{id} AND status = 'pending' AND version = #{version}")
    int cancelResale(@Param("id") Long id, @Param("version") Integer version);

    /**
     * 调整转售价（仅 pending）
     */
    @Update("UPDATE resale_record SET resale_price = #{resalePrice}, artist_income = #{artistIncome}, " +
            "platform_fee = #{platformFee}, seller_income = #{sellerIncome}, version = version + 1, updated_time = NOW() " +
            "WHERE id = #{id} AND status = 'pending' AND version = #{version}")
    int updateResalePrice(@Param("id") Long id,
                          @Param("resalePrice") BigDecimal resalePrice,
                          @Param("artistIncome") BigDecimal artistIncome,
                          @Param("platformFee") BigDecimal platformFee,
                          @Param("sellerIncome") BigDecimal sellerIncome,
                          @Param("version") Integer version);
}
