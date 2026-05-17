package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作品交易链路记录
 * 记录作品完整流通链路：首次出售 + 每次转售
 */
@Data
@TableName("artwork_trade_record")
public class ArtworkTradeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作品ID */
    private Long artworkId;

    /** 交易编号 */
    private String tradeNo;

    /** 卖家用户ID（首次出售可为空，表示平台或艺术家自售） */
    private Long sellerUserId;

    /** 买家用户ID */
    private Long buyerUserId;

    /** 成交价格 */
    private BigDecimal tradePrice;

    /** 交易类型: first_sale / resale */
    private String tradeType;

    /** 交易轮次（1=首次出售，2=第一次转售，依此类推） */
    private Integer tradeRound;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
