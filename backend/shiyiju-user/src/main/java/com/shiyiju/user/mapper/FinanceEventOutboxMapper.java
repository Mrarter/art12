package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.FinanceEventOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 金融事件 Outbox Mapper
 */
@Mapper
public interface FinanceEventOutboxMapper extends BaseMapper<FinanceEventOutbox> {

    /**
     * 批量拉取待处理事件
     */
    @Select("SELECT * FROM finance_event_outbox WHERE status = 'PENDING' " +
            "AND (next_retry_time IS NULL OR next_retry_time <= NOW()) " +
            "ORDER BY created_time ASC LIMIT #{limit} FOR UPDATE SKIP LOCKED")
    List<FinanceEventOutbox> pollPendingEvents(@Param("limit") int limit);

    /**
     * 标记为处理中
     */
    @Update("UPDATE finance_event_outbox SET status = 'PROCESSING' WHERE id = #{id} AND status = 'PENDING'")
    int markProcessing(@Param("id") Long id);

    /**
     * 标记为已完成
     */
    @Update("UPDATE finance_event_outbox SET status = 'COMPLETED', completed_time = NOW() WHERE id = #{id}")
    int markCompleted(@Param("id") Long id);

    /**
     * 标记为失败（更新重试次数）
     */
    @Update("UPDATE finance_event_outbox SET status = 'FAILED', retry_count = #{retryCount}, " +
            "error_message = #{errorMsg}, next_retry_time = #{nextRetry} WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("retryCount") Integer retryCount,
                   @Param("errorMsg") String errorMsg, @Param("nextRetry") LocalDateTime nextRetry);
}
