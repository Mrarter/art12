package com.shiyiju.user.vo;

import lombok.Data;

/**
 * 艺术家认证状态 VO
 */
@Data
public class ArtistCertStatusVO {

    /** 认证状态：0-待审核，1-已通过，2-已拒绝，null-未申请 */
    private Integer status;

    /** 状态文本 */
    private String statusText;

    /** 拒绝原因（如果被拒绝） */
    private String rejectReason;

    /** 是否已认证艺术家 */
    private Boolean isArtist;

    /** 审核时间 */
    private String reviewTime;
}
