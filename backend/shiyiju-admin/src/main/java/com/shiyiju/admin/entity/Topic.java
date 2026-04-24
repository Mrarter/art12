package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 话题实体
 */
@Data
public class Topic {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String coverImage;
    private Integer postCount;
    private Integer followCount;
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
