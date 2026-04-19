package com.shiyiju.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信登录 DTO
 */
@Data
public class WxLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 微信授权码 */
    private String code;

    /** 用户信息加密数据 */
    private String encryptedData;

    /** 加密算法的初始向量 */
    private String iv;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 性别：0-未知，1-男，2-女 */
    private Integer gender;

    /** 生日 */
    private String birthday;

    /** 地区 */
    private String region;

    /** 邀请码 */
    private String inviteCode;
}
