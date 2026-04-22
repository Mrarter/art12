package com.shiyiju.order.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class OrderVO implements Serializable {
    private Long id;
    private String orderNo;
    
    /** 商品总金额 */
    @JsonProperty("goodsAmount")
    private Long totalAmount;
    
    private Long discountAmount;
    
    /** 实付金额 */
    private Long payAmount;
    
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
    
    /** 卖家头像 */
    private String sellerAvatar;
    
    /** 卖家名称 */
    private String sellerName;
    
    /** 运费 */
    private Long freight;
    
    /** 物流单号 */
    private String trackingNo;
    
    /** 物流公司 */
    private String expressName;
}
