package com.shiyiju.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 添加收款账户 DTO
 */
@Data
public class PayAccountAddDTO {

    /** 账户类型：1-微信 2-支付宝 3-银行卡 */
    @NotNull(message = "账户类型不能为空")
    private Integer accountType;

    /** 收款人姓名 */
    @NotBlank(message = "收款人姓名不能为空")
    private String realName;

    /** 身份证号 */
    @Pattern(regexp = "^$|^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    /** 手机号 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 开户银行（银行卡类型时必填） */
    private String bankName;

    /** 银行卡号 */
    private String bankCard;

    /** 支付宝账号 */
    private String alipayAccount;

    /** 微信OpenId */
    private String wechatOpenid;

    /** 是否设为默认 */
    private Boolean setDefault;
}
