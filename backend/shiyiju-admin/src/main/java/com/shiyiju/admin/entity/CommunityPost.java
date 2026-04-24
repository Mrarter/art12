package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区帖子实体
 */
@Data
public class CommunityPost {
    private Long id;
    private Long userId;
    private String nickname;
    private String content;
    private String images;
    private String videoUrl;
    private Long topicId;
    private Long artworkId;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer status;
    private String auditRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
