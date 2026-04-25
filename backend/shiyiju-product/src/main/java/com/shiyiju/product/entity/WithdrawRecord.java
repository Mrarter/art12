package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现记录表实体
 * 对应表: withdraw_record
 */
@Data
@TableName("withdraw_record")
public class WithdrawRecord implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 提现标准化编号 (19位: WTH + 日期 + 序列 + 随机码) */
    @TableField("withdraw_code")
    private String withdrawCode;
    
    private Long userId; // 用户ID
    private BigDecimal amount; // 提现金额
    private Integer withdrawType; // 提现方式: 1-微信, 2-支付宝, 3-银行卡
    private String account; // 收款账户
    private String realName; // 真实姓名
    private String bankName; // 银行名称(银行卡时)
    private String bankBranch; // 支行名称
    private Integer status; // 状态: 0-待处理, 1-已通过, 2-已拒绝, 3-已打款
    private String rejectReason; // 拒绝原因
    private LocalDateTime applyTime; // 申请时间
    private LocalDateTime handleTime; // 处理时间
    private LocalDateTime completeTime; // 完成时间
    private String transactionId; // 交易流水号
    private Long operatorId; // 操作员ID
    private String operatorName; // 操作员姓名
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
