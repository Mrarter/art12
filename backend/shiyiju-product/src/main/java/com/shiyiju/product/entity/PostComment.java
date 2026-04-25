package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论表实体
 * 对应表: post_comment
 */
@Data
@TableName("post_comment")
public class PostComment implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 评论标准化编号 (19位: CMT + 日期 + 序列 + 随机码) */
    @TableField("comment_code")
    private String commentCode;
    
    private Long postId; // 帖子ID
    private Long userId; // 评论用户ID
    private Long parentId; // 父评论ID(回复)
    private String content; // 评论内容
    private Integer likeCount; // 点赞数
    private Integer status; // 状态: 0-删除, 1-正常
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
