package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.MqConsumeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MQ 消费日志 Mapper
 */
@Mapper
public interface MqConsumeLogMapper extends BaseMapper<MqConsumeLog> {

    @Select("SELECT * FROM mq_consume_log WHERE msg_id = #{msgId} LIMIT 1")
    MqConsumeLog findByMsgId(@Param("msgId") String msgId);

    @Select("SELECT * FROM mq_consume_log WHERE txn_id = #{txnId} AND status = 'CONSUMED' LIMIT 1")
    MqConsumeLog findConsumedByTxnId(@Param("txnId") String txnId);
}
