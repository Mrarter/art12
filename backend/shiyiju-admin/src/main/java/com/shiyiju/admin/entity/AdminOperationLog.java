package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员操作日志实体
 */
@Data
public class AdminOperationLog {
    private Long id;
    private Long adminId;
    private String adminName;
    private String operation;
    private String module;
    private String detail;
    private String ip;
    private String userAgent;
    private LocalDateTime createTime;
}
