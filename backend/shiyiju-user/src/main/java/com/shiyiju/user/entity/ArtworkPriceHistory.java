package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作品价格历史记录
 * 记录作品每次价格变动，自动生成价格成长曲线
 */
@Data
@TableName("artwork_price_history")
public class ArtworkPriceHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作品ID */
    private Long artworkId;

    /** 变动前价格 */
    private BigDecimal beforePrice;

    /** 变动后价格（成交价） */
    private BigDecimal afterPrice;

    /** 涨幅(%) */
    private BigDecimal growthRate;

    /** 变动原因: first_sale / resale / admin_adjust */
    private String reason;

    /** 关联转售记录ID（如果是转售触发的） */
    private Long relatedResaleId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
