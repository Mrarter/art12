package com.yixiangji.artwork.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Artwork {
    private Long id;
    private Long artistId;
    private String title;
    private BigDecimal initialPrice;
    private BigDecimal currentPrice;
    private String status;
    private Integer collectCount;
    private Integer saleCount;
    private LocalDateTime lastSaleTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
