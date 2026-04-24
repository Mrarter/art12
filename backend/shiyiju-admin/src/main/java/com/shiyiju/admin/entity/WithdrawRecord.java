package com.shiyiju.admin.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现记录实体
 */
@Data
public class WithdrawRecord {
    private Long id;
    private Long userId;
    private String nickname;
    private BigDecimal amount;
    private Integer withdrawType;
    private String account;
    private String realName;
    private String bankName;
    private String bankBranch;
    private Integer status;
    private String rejectReason;
    private LocalDateTime applyTime;
    private LocalDateTime handleTime;
    private LocalDateTime completeTime;
    private String transactionId;
    private Long operatorId;
    private String operatorName;
}
