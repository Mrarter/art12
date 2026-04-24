package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 后台菜单实体
 */
@Data
public class AdminMenu {
    private Long id;
    private String name;
    private String path;
    private String icon;
    private Integer sort;
    private Long parentId;
    private Integer level;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
