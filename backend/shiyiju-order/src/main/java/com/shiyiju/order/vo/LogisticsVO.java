package com.shiyiju.order.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流信息VO
 */
@Data
public class LogisticsVO {

    private Long id;

    private Long orderId;

    private String companyCode;

    private String companyName;

    private String trackingNo;

    private String shipTime;

    private String receiveTime;

    private Integer status;

    private String statusText;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    /**
     * 物流轨迹列表
     */
    private List<LogisticsTrackVO> tracks;
}
