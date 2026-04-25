package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖拍品表实体
 * 对应表: auction_lot
 */
@Data
@TableName("auction_lot")
public class AuctionLot implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 拍品标准化编号 (19位: LOT + 日期 + 序列 + 随机码) */
    @TableField("lot_code")
    private String lotCode;
    
    private Long sessionId; // 专场ID
    private Long artworkId; // 关联作品ID
    private String lotNo; // 拍品号
    private String title; // 拍品名称
    private String coverImage;
    private String artistName; // 艺术家
    private BigDecimal startPrice; // 起拍价
    private BigDecimal reservePrice; // 保留价
    private BigDecimal currentPrice; // 当前价
    private String estimatePrice; // 估价
    private BigDecimal increment; // 加价幅度
    private BigDecimal depositAmount; // 保证金金额
    private Integer bidCount; // 出价次数
    private Long buyerId; // 当前买家ID
    private Integer status; // 状态: 0-未开始, 1-竞拍中, 2-已成交, 3-流拍, 4-撤拍
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
