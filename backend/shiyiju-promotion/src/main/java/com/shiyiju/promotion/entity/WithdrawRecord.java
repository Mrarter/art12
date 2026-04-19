package com.shiyiju.promotion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("withdraw_records")
public class WithdrawRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promoterId;
    private Long amount;
    private Long feeAmount;
    private Long actualAmount;
    private String accountType;
    private String accountInfo;
    private String accountName;
    private Integer status;
    private String rejectReason;
    private LocalDateTime processTime;
    private LocalDateTime transferTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
