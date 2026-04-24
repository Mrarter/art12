package com.shiyiju.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.admin.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台管理员Mapper
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
