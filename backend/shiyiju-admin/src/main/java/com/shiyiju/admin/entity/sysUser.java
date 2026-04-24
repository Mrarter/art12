package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体（复用业务表）
 */
@Data
public class sysUser {
    private Long id;
    private String nickname;
    private String phone;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
}
