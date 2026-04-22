package com.shiyiju.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单项实体
 */
@Data
@TableName("trade_order_item")
public class OrderItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 订单ID */
    @TableField("order_id")
    private Long orderId;
    
    /** 艺术品ID */
    @TableField("artwork_id")
    private Long artworkId;
    
    /** 艺术家ID */
    @TableField("artist_id")
    private Long artistId;
    
    /** 物品类型 */
    @TableField("item_type")
    private String itemType;
    
    /** SKU编码 */
    private String skuNo;
    
    /** 物品标题 */
    @TableField("item_title")
    private String title;
    
    /** 艺术家名称 (非数据库字段) */
    @TableField(exist = false)
    private String authorName;
    
    /** 封面图 */
    private String coverImage;
    
    /** 单价 */
    private Long price;
    
    /** 数量 */
    private Integer quantity;
    
    /** 小计金额 */
    private Long subtotal;
    
    /** 归属艺荐官ID */
    private Long promoterId;
    
    /** 佣金状态：0-未结算，1-已结算 */
    private Integer commissionStatus;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
