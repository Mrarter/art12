package com.shiyiju.order.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class OrderVO implements Serializable {
    private Long id;
    private String orderNo;
    private Long totalAmount;
    private Long discountAmount;
    private Long payAmount;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private String source;
    private String sourceText;
    private String status;
    private String statusText;
    private String payTime;
    private String shipTime;
    private String receiveTime;
    private String createTime;
    private List<OrderItemVO> items;
}
