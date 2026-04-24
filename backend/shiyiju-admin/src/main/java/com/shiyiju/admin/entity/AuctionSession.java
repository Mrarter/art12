package com.shiyiju.admin.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖专场实体
 */
@Data
public class AuctionSession {
    private Long id;
    private String title;
    private String coverImage;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Integer totalLots;
    private Integer totalBids;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
