package com.shiyiju.order.vo;

import lombok.Data;

/**
 * 物流轨迹VO
 */
@Data
public class LogisticsTrackVO {

    private String trackTime;

    private String status;

    private String description;

    private String location;
}
