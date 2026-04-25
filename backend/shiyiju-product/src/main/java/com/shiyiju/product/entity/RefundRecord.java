package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款/售后记录表实体
 * 对应表: refund_record
 */
@Data
@TableName("refund_record")
public class RefundRecord implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 售后标准化编号 (19位: ASM + 日期 + 序列 + 随机码) */
    @TableField("refund_code")
    private String refundCode;
    
    private Long orderId; // 订单ID
    private String orderNo; // 订单号
    private Long userId; // 用户ID
    private BigDecimal refundAmount; // 退款金额
    private Integer refundType; // 退款方式: 1-原路退回, 2-退回余额
    private String reason; // 退款原因
    private String images; // 凭证图片JSON
    private Integer status; // 状态: 0-待处理, 1-同意, 2-拒绝
    private String handleRemark; // 处理备注
    private LocalDateTime applyTime; // 申请时间
    private LocalDateTime handleTime; // 处理时间
    private LocalDateTime completeTime; // 完成时间
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
