package com.shiyiju.admin.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体
 */
@Data
public class RefundRecord {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String nickname;
    private String artworkTitle;
    private BigDecimal refundAmount;
    private Integer refundType;
    private String reason;
    private String images;
    private Integer status;
    private String handleRemark;
    private LocalDateTime applyTime;
    private LocalDateTime handleTime;
    private LocalDateTime completeTime;
}
