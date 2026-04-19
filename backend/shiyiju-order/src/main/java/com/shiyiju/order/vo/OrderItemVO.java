package com.shiyiju.order.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderItemVO implements Serializable {
    private Long id;
    private Long artworkId;
    private String title;
    private String coverImage;
    private String authorName;
    private Long price;
    private Integer quantity;
    private Long subtotal;
}
