package com.shiyiju.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Account security overview for the user settings page.
 */
@Data
public class AccountSecurityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean phoneBound;
    private String phoneMasked;
    private Boolean passwordSet;
    private Boolean wechatBound;
    private String lastLoginTime;
    private String registerTime;
    private String securityLevel;
    private Integer securityScore;
    private List<String> tips;
}
