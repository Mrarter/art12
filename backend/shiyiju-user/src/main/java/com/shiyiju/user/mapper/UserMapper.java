package com.shiyiju.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（与 common 模块的 UserMapper 区分，使用不同 bean 名）
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
