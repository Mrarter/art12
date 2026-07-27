package com.shiyiju.order.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CartVO implements Serializable {
    private Long id;
    
    @JsonProperty("productId")
    private Long artworkId;
    
    private String title;
    
    @JsonProperty("cover")
    private String coverImage;
    
    @JsonProperty("artistName")
    private String authorName;
    
    private String size;
    /** 价格（元） */
    private BigDecimal price;
    /** 原始价格（元） */
    private BigDecimal originalPrice;
    
    @JsonProperty("num")
    private Integer quantity;
    
    /** 小计（元） */
    private BigDecimal subtotal;
    private Integer stock;
    private Boolean selected;
    
    /** 作品是否锁定（结算中） */
    private Boolean locked;
    
    /** 卖家ID */
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private Long publisherId;
    private String publisherName;
    private String publisherAvatar;
    private Long promoterId;
    private String promoterName;
    private String promoterAvatar;
}
