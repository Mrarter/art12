package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 话题表实体
 * 对应表: topic
 */
@Data
@TableName("topic")
public class Topic implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 话题标准化编号 (19位: TOP + 日期 + 序列 + 随机码) */
    @TableField("topic_code")
    private String topicCode;
    
    private String name; // 话题名称
    private String description; // 话题描述
    private String icon; // 话题图标
    private String coverImage; // 封面图
    private Integer postCount; // 帖子数
    private Integer followCount; // 关注数
    private Integer status; // 状态: 0-禁用, 1-启用
    private Integer sort; // 排序
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
