package com.shiyiju.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

/**
 * 用户Mapper (common模块)
 */
@Mapper
@Component("commonUserMapper")
public interface UserMapper extends BaseMapper<User> {
}
