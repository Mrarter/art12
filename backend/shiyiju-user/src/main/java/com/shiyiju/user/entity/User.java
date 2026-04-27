package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户标准化UID (19位: USR + 日期 + 序列 + 随机码) */
    private String uid;

    /** 微信OpenID */
    private String openid;

    /** 微信 UnionID */
    private String unionid;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 性别：0-未知，1-男，2-女 */
    private Integer gender;

    /** 生日 */
    private String birthday;

    /** 简介 */
    private String bio;

    /** 地区 */
    private String region;

    /** 身份列表，逗号分隔：artist,agent,collector,promoter */
    private String identities;

    /** 用户状态：0-禁用，1-正常 */
    private Integer status;

    /** 粉丝数 */
    private Integer followerCount;

    /** 关注数 */
    private Integer followingCount;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
