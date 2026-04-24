package com.shiyiju.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 */
@Data
public class SystemConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String configType;
    private String groupName;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
