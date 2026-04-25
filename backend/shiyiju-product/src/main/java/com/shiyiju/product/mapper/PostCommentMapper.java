package com.shiyiju.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.product.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论Mapper
 */
@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {
}
