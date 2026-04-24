package com.shiyiju.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表Mapper（复用业务表）
 */
@Mapper
public interface sysUserMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<com.shiyiju.admin.entity.sysUser> {
}
