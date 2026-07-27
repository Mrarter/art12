package com.shiyiju.order.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.List;

@Data
public class OrderVO implements Serializable {
    private Long id;
    private String orderNo;
    
    /** 商品总金额 */
    @JsonProperty("goodsAmount")
    private BigDecimal totalAmount;
    
    private BigDecimal discountAmount;
    
    /** 实付金额 */
    private BigDecimal payAmount;
    
    /** 收货人信息 */
    private AddressVO address;
    
    private String remark;
    private String source;
    private String sourceText;
    
    /** 订单状态 */
    private String status;
    private String statusText;
    private String payTime;
    
    /** 发货时间 */
    @JsonProperty("deliveryTime")
    private String shipTime;
    
    /** 收货时间 */
    @JsonProperty("completeTime")
    private String receiveTime;
    
    private String createTime;
    
    /** 订单商品列表 - 前端期望 goodsList */
    @JsonProperty("goodsList")
    private List<OrderItemVO> items;

    /** 买家ID */
    private Long buyerUserId;

    /** 买家头像 */
    private String buyerAvatar;

    /** 买家名称 */
    private String buyerName;
    
    /** 卖家头像 */
    private String sellerAvatar;
    
    /** 卖家名称 */
    private String sellerName;
    
    /** 运费 */
    private BigDecimal freight;
    
    /** 物流单号 */
    private String trackingNo;
    
    /** 物流公司 */
    private String expressName;

    /** 退款类型：1-仅退款 2-退货退款 */
    private Integer refundType;

    /** 退款记录状态：0-待处理 1-同意 2-拒绝 */
    private Integer refundStatus;

    /** 退款原因 */
    private String refundReason;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 退款凭证 */
    private String refundImages;

    /** 退货回寄物流公司编码 */
    private String returnCompanyCode;

    /** 退货回寄物流公司 */
    private String returnCompanyName;

    /** 退货回寄运单号 */
    private String returnTrackingNo;

    /** 退货回寄物流状态：1-已寄回 2-运输中 3-派送中 4-已签收 5-拒收 6-退件 */
    private Integer returnStatus;

    /** 退货回寄时间 */
    private String returnShipTime;

    /** 退货签收时间 */
    private String returnReceiveTime;
}
