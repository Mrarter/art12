package com.shiyiju.user.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 艺术家认证身份证识别结果
 */
@Data
public class ArtistIdCardVerifyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean valid;
    private String cardSide;
    private String realName;
    private String idCard;
    private String authority;
    private String validDate;
    private String message;
}
