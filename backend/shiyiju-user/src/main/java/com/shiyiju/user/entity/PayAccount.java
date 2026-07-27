package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收款账户表
 */
@Data
@TableName("pay_account")
public class PayAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 账户类型：1-微信 2-支付宝 3-银行卡 */
    private Integer accountType;

    /** 收款人姓名 */
    private String realName;

    /** 身份证号（脱敏存储） */
    private String idCard;

    /** 手机号 */
    private String phone;

    /** 开户银行 */
    private String bankName;

    /** 银行卡号（AES加密存储） */
    private String bankCard;

    /** 支付宝账号 */
    private String alipayAccount;

    /** 微信OpenId */
    private String wechatOpenid;

    /** 是否默认账户：0-否 1-是 */
    private Integer isDefault;

    /** 实名认证状态：0-未认证 1-已认证 */
    private Integer verifyStatus;

    /** 状态：1-正常 0-禁用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
