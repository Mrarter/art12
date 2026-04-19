package com.shiyiju.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.common.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货地址Mapper - 跨模块共享
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
