package com.shiyiju.admin.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖拍品实体
 */
@Data
public class AuctionLot {
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
