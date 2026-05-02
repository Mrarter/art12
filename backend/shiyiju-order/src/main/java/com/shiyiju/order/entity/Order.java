package com.shiyiju.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("trade_order")
public class Order implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String orderNo;
    
    /** 买家用户ID */
    @TableField("buyer_user_id")
    private Long userId;
    
    /** 订单类型: DIRECT(立即购买), CART(购物车), AUCTION(拍卖) */
    @TableField("order_type")
    private String source;
    
    /** 订单状态: PENDING_PAYMENT/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDING/REFUNDED */
    @TableField("order_status")
    private String status;
    
    /** 支付状态: UNPAID/PAID/REFUNDING/REFUNDED */
    @TableField("payment_status")
    private String paymentStatus;
    
    /** 收货地址ID */
    private Long addressId;
    
    /** 商品总额 */
    @TableField("goods_amount")
    private BigDecimal totalAmount;
    
    /** 运费 */
    @TableField("freight_amount")
    private BigDecimal freightAmount;
    
    /** 折扣金额 */
    @TableField("discount_amount")
    private BigDecimal discountAmount;
    
    /** 应付金额 */
    @TableField("pay_amount")
    private BigDecimal payAmount;
    
    /** 备注 */
    private String remark;
    
    /** 支付时间 */
    @TableField("paid_at")
    private LocalDateTime payTime;
    
    /** 取消时间 */
    @TableField("cancelled_at")
    private LocalDateTime cancelTime;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    // ===== 以下字段为方便前端使用，不对应数据库字段 =====
    
    /** 发货时间 (非数据库字段) */
    @TableField(exist = false)
    private LocalDateTime shipTime;
    
    /** 收货时间 (非数据库字段) */
    @TableField(exist = false)
    private LocalDateTime receiveTime;
    
    /** 收货人姓名 (非数据库字段) */
    @TableField(exist = false)
    private String receiverName;
    
    /** 收货人电话 (非数据库字段) */
    @TableField(exist = false)
    private String receiverPhone;
    
    /** 收货地址 (非数据库字段) */
    @TableField(exist = false)
    private String receiverAddress;
    
    /** 佣金金额 (非数据库字段) */
    @TableField(exist = false)
    private BigDecimal commissionAmount;
    
    /** 推广员ID (非数据库字段) */
    @TableField(exist = false)
    private Long promoterId;
    
    /** 卖家名称 (非数据库字段) */
    @TableField(exist = false)
    private String sellerName;
    
    /** 卖家头像 (非数据库字段) */
    @TableField(exist = false)
    private String sellerAvatar;
    
    /** 物流单号 (非数据库字段) */
    @TableField(exist = false)
    private String trackingNo;
    
    /** 物流公司 (非数据库字段) */
    @TableField(exist = false)
    private String expressName;
}
