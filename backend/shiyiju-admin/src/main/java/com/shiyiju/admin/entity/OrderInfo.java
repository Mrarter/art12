package com.shiyiju.admin.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体
 */
@Data
public class OrderInfo {
    private Long id;
    private String orderNo;
    private Long userId;
    private String nickname;
    private String phone;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private BigDecimal commissionAmount;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private Integer source;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime receiveTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private String expressCompany;
    private String expressNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
