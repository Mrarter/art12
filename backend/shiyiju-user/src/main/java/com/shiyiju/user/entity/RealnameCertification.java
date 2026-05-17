package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实名认证表
 */
@Data
@TableName("realname_certifications")
public class RealnameCertification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 身份证号（脱敏存储） */
    private String idCard;

    /** 身份证号SHA256（查重用） */
    private String idCardHash;

    /** 身份证正面照URL */
    private String idFrontUrl;

    /** 身份证背面照URL */
    private String idBackUrl;

    /** 人脸核验状态：0-未核验，1-已通过 */
    private Integer faceVerified;

    /** 审核状态：0-待审核，1-已通过，2-已拒绝 */
    private Integer status;

    /** 拒绝原因 */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 审核人ID */
    private Long reviewerId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
