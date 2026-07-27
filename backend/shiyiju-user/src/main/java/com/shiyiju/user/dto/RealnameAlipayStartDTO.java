package com.shiyiju.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 支付宝实名认证发起 DTO
 */
@Data
public class RealnameAlipayStartDTO {

    /** 可选；为空时优先读取后台已保存的实名姓名 */
    private String realName;

    /** 可选；为空时优先读取后台已保存的完整身份证号 */
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    /** 支付宝认证完成后的回跳地址；为空时使用系统默认实名认证页 */
    private String returnUrl;

    /** 是否强制重新发起一次支付宝实名认证 */
    private Boolean restart;
}
