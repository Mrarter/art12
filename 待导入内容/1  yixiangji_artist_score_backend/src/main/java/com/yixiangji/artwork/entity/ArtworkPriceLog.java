package com.yixiangji.artwork.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ArtworkPriceLog {
    private Long id;
    private Long artworkId;
    private Long artistId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal changeRate;
    private String changeReason;
    private String remark;
    private LocalDateTime createdAt;
}
