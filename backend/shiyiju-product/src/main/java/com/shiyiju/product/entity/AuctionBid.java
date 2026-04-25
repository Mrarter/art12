package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖出价记录表实体
 * 对应表: auction_bid
 */
@Data
@TableName("auction_bid")
public class AuctionBid implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 竞拍标准化编号 (19位: BID + 日期 + 序列 + 随机码) */
    @TableField("bid_code")
    private String bidCode;
    
    private Long lotId; // 拍品ID
    private Long userId; // 出价用户ID
    private BigDecimal bidPrice; // 出价金额
    private LocalDateTime bidTime; // 出价时间
    private Integer status; // 状态: 0-出局, 1-领先
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
