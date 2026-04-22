package com.shiyiju.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流轨迹实体
 */
@Data
@TableName("logistics_track")
public class LogisticsTrack {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 物流ID
     */
    private Long logisticsId;

    /**
     * 运单号
     */
    private String trackingNo;

    /**
     * 物流时间
     */
    private LocalDateTime trackTime;

    /**
     * 物流状态
     */
    private String status;

    /**
     * 物流描述
     */
    private String description;

    /**
     * 当前地点
     */
    private String location;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
