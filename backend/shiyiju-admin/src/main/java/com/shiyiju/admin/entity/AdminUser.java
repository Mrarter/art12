package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 后台管理员实体
 */
@Data
public class AdminUser {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String avatar;
    private Long roleId;
    private String roleName;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
