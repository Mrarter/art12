package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 * 对应表: sys_user
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户标准化UID (19位: USR + 日期 + 序列 + 随机码) */
    @TableField("uid")
    private String uid;
    
    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer gender; // 性别: 0-未知, 1-男, 2-女
    private Integer status; // 状态: 0-禁用, 1-正常
    private String identity; // 当前身份: artist/agent/collector/promoter
    private String identityJson; // 身份JSON数组
    private String inviteCode; // 个人邀请码
    private Long parentId; // 上级用户ID
    private Integer experienceYears; // 从业年限(艺术家)
    private String artistLevel; // 艺术家等级
    private String promoterLevel; // 艺荐官等级
    private java.math.BigDecimal totalCommission; // 累计佣金
    private java.math.BigDecimal availableCommission; // 可提现佣金
    private LocalDateTime lastLoginTime; // 最后登录时间
    private String lastLoginIp; // 最后登录IP
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
