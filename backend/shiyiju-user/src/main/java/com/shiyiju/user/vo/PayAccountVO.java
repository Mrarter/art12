package com.shiyiju.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收款账户 VO（返回前端时已脱敏）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayAccountVO {

    private Long id;

    /** 账户类型：1-微信 2-支付宝 3-银行卡 */
    private Integer accountType;

    /** 账户类型文字 */
    private String accountTypeText;

    /** 收款人姓名（脱敏：张**） */
    private String realName;

    /** 身份证号（脱敏：410***********1234） */
    private String idCard;

    /** 手机号（脱敏：138****1234） */
    private String phone;

    /** 开户银行 */
    private String bankName;

    /** 银行卡号（脱敏：622202******1234） */
    private String bankCard;

    /** 支付宝账号（脱敏） */
    private String alipayAccount;

    /** 微信OpenId */
    private String wechatOpenid;

    /** 是否默认 */
    private Boolean isDefault;

    /** 实名认证状态 */
    private Integer verifyStatus;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 账户图标名 */
    private String icon;
}
