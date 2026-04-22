package com.shiyiju.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.file.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息 Mapper
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
}
