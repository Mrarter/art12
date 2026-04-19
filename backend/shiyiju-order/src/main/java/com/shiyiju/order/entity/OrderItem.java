package com.shiyiju.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("order_items")
public class OrderItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long artworkId;
    private String title;
    private String coverImage;
    private String authorName;
    private Long price;
    private Integer quantity;
    private Long subtotal;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
