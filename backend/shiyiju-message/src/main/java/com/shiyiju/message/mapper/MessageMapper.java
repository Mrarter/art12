package com.shiyiju.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {}
