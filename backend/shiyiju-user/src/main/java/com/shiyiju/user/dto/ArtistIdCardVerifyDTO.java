package com.shiyiju.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 艺术家认证身份证识别请求
 */
@Data
public class ArtistIdCardVerifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "身份证图片不能为空")
    private String imageBase64;

    @NotBlank(message = "证件面类型不能为空")
    @Pattern(regexp = "^(front|back)$", message = "证件面类型不正确")
    private String cardSide;
}
