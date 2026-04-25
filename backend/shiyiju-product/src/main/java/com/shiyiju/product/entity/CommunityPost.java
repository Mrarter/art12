package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区帖子表实体
 * 对应表: community_post
 */
@Data
@TableName("community_post")
public class CommunityPost implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 帖子标准化编号 (19位: POST + 日期 + 序列 + 随机码) */
    @TableField("post_code")
    private String postCode;
    
    private Long userId; // 发布用户ID
    private String content; // 帖子内容
    private String images; // 图片列表JSON
    private String videoUrl; // 视频URL
    private Long topicId; // 话题ID
    private Long artworkId; // 关联作品ID
    private Integer likeCount; // 点赞数
    private Integer commentCount; // 评论数
    private Integer shareCount; // 分享数
    private Integer status; // 状态: 0-删除, 1-正常, 2-审核中, 3-违规
    private String auditRemark; // 审核备注
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
