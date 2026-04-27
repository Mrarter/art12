package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体（复用业务表）
 */
@Data
public class SysUser {
    private Long id;
    
    /** 用户标准化UID (19位: USR + 日期 + 序列 + 随机码) */
    private String uid;
    
    private String nickname;
    private String phone;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
}
