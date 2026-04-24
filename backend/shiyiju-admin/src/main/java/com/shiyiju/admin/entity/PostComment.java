package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论实体
 */
@Data
public class PostComment {
    private Long id;
    private Long postId;
    private Long userId;
    private String nickname;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
