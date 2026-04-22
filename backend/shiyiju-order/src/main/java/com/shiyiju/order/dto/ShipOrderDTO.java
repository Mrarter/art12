package com.shiyiju.order.dto;

import lombok.Data;

/**
 * 订单发货DTO
 */
@Data
public class ShipOrderDTO {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 物流公司编码
     */
    private String companyCode;

    /**
     * 物流公司名称
     */
    private String companyName;

    /**
     * 运单号
     */
    private String trackingNo;

    /**
     * 发货备注
     */
    private String remark;
}
