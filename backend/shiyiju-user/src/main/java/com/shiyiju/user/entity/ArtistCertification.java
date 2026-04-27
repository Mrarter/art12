package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 艺术家认证表
 */
@Data
@TableName("artist_certifications")
public class ArtistCertification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 艺术家认证编号 (如: ART202604200001K9M3) */
    private String artistCode;

    /** 用户ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 身份证号 */
    private String idCard;

    /** 个人履历 */
    private String resume;

    /** 代表作图片URLs，逗号分隔 */
    private String artworks;

    /** 参展证明URLs，逗号分隔 */
    private String exhibits;

    /** 认证状态：0-待审核，1-已通过，2-已拒绝 */
    private Integer status;

    /** 拒绝原因 */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
