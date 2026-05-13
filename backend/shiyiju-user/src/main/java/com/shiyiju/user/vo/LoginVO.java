package com.shiyiju.user.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录结果 VO
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Token */
    private String token;

    /** 是否新用户 */
    private Boolean isNewUser;

    /** 用户ID */
    private Long userId;

    /** 用户标准UID */
    private String uid;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 身份列表 */
    private String identities;
    
    /** 微信openId（用于JSAPI支付） */
    private String openId;
}
