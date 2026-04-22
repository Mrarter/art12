package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价回复实体
 */
@Data
@TableName("review_reply")
public class ReviewReply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 评价ID
     */
    private Long reviewId;

    /**
     * 回复用户ID
     */
    private Long userId;

    /**
     * 回复用户昵称
     */
    private String userNickname;

    /**
     * 回复用户头像
     */
    private String userAvatar;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 回复类型（buyer-买家回复/seller-卖家回复/admin-平台回复）
     */
    private String replyType;

    /**
     * 被回复的用户ID（用于@功能）
     */
    private Long replyToUserId;

    /**
     * 被回复的用户昵称
     */
    private String replyToNickname;

    /**
     * 审核状态（0-待审核 1-通过 2-拒绝）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
