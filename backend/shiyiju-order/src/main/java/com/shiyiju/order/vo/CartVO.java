package com.shiyiju.order.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class CartVO implements Serializable {
    private Long id;
    private Long artworkId;
    private String title;
    private String coverImage;
    private String authorName;
    private String size;
    private Long price;
    private Integer quantity;
    private Long subtotal;
    private Integer stock;
    private Boolean selected;
}
