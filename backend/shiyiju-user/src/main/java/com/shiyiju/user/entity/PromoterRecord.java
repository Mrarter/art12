package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 艺荐官开通记录表
 */
@Data
@TableName("promoter_records")
public class PromoterRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 邀请人ID */
    private Long inviterId;

    /** 邀请码 */
    private String inviteCode;

    /** 艺荐官等级：1-普通，2-高级，3-资深 */
    private Integer level;

    /** 协议签署状态：0-未签署，1-已签署 */
    private Integer agreementStatus;

    /** 协议签署时间 */
    private LocalDateTime agreementTime;

    /** 累计佣金 */
    private Long totalCommission;

    /** 可提现佣金 */
    private Long withdrawableCommission;

    /** 已提现佣金 */
    private Long withdrawnCommission;

    /** 下级人数 */
    private Integer subordinateCount;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
