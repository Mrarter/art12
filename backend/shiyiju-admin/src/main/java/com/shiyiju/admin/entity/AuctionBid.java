package com.shiyiju.admin.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖出价记录实体
 */
@Data
public class AuctionBid {
    private Long id;
    private Long lotId;
    private Long userId;
    private String nickname;
    private BigDecimal bidPrice;
    private LocalDateTime bidTime;
    private Integer status;
    private LocalDateTime createTime;
}
