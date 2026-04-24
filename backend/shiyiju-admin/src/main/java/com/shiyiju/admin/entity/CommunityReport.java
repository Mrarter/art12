package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区举报实体
 */
@Data
public class CommunityReport {
    private Long id;
    private Long reporterId;
    private String reporterNickname;
    private Long reportedUserId;
    private String reportedNickname;
    private Long targetId;
    private String targetType;
    private String reportType;
    private String content;
    private String images;
    private Integer status;
    private String handleResult;
    private Long handlerId;
    private String handlerName;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}
