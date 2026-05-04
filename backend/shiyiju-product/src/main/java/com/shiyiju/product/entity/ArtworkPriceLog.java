package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("artwork_price_log")
public class ArtworkPriceLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("artwork_id")
    private Long artworkId;

    @TableField("artist_id")
    private Long artistId;

    @TableField("old_price")
    private Long oldPrice;

    @TableField("new_price")
    private Long newPrice;

    @TableField("change_rate")
    private BigDecimal changeRate;

    @TableField("change_reason")
    private String changeReason;

    private String remark;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
