package com.shiyiju.auction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("auction_lot")
public class AuctionLot implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long artworkId;
    private String lotNo;
    private String title;
    private String coverImage;
    private String artistName;
    private BigDecimal startPrice;
    private BigDecimal reservePrice;
    private BigDecimal currentPrice;
    private String estimatePrice;
    private BigDecimal increment;
    private BigDecimal depositAmount;
    private Integer bidCount;
    private Long buyerId;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
