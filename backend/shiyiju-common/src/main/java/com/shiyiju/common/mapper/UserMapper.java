package com.shiyiju.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
