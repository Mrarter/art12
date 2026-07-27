package com.shiyiju.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Account security operation payload.
 */
@Data
public class AccountSecurityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** New phone number for bind/change-phone flow. */
    private String phone;

    /** SMS verification code. */
    private String code;

    /** Current password, required when changing an existing password. */
    private String currentPassword;

    /** New login password. */
    private String newPassword;
}
