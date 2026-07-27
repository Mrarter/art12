package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.FinanceEventDlq;
import org.apache.ibatis.annotations.Mapper;

/**
 * 金融事件死信队列 Mapper
 */
@Mapper
public interface FinanceEventDlqMapper extends BaseMapper<FinanceEventDlq> {

}
