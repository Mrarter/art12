package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 艺术家档案实体
 * 对应 artist_profile 表
 */
@Data
@TableName("artist_profile")
public class ArtistProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID (数字格式)
     */
    private Long userId;

    /**
     * 用户UID (19位字符串格式)
     */
    private String userUid;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 艺术家名称
     */
    private String artistName;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 简历/经历
     */
    private String resume;

    /**
     * 状态: 0-待审核, 1-已认证, 2-已拒绝
     */
    private Integer status;

    /**
     * 认证等级
     */
    private String artistLevel;

    /**
     * 艺术家编号
     */
    private String artistCode;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
