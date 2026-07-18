package com.shiyiju.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付宝实名认证发起结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealnameAlipayStartVO {

    private String certifyId;

    private String redirectUrl;

    /** 是否已经完成认证，无需再次跳转支付宝 */
    private Boolean verified;
}
