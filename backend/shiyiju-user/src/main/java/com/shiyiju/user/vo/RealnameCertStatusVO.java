package com.shiyiju.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 实名认证状态 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealnameCertStatusVO {

    /** 审核状态：0-未认证，1-已通过，2-审核中，3-已拒绝 */
    private Integer status;

    /** 认证模式：manual/alipay */
    private String verifyMode;

    /** 是否已启用支付宝实名认证 */
    private Boolean alipayEnabled;

    /** 最近一次支付宝认证流水号 */
    private String certifyId;

    /** 脱敏真实姓名（如：张**） */
    private String maskedRealName;

    /** 脱敏身份证号（如：410***********1234） */
    private String maskedIdCard;

    /** 拒绝原因 */
    private String rejectReason;

    /** 提交时间 */
    private LocalDateTime submittedAt;

    /** 审核时间 */
    private LocalDateTime reviewTime;
}
