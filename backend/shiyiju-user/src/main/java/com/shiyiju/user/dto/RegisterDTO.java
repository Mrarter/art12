package com.shiyiju.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 用户注册 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号 */
    private String phone;

    /** 短信验证码 */
    private String code;

    /** 登录密码 */
    private String password;

    /** 用户昵称（可选） */
    private String nickname;

    /** 邀请码（可选） */
    private String inviteCode;
}
