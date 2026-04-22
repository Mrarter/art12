package com.shiyiju.order.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;

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
    private Long price;
    private Long originalPrice;
    
    @JsonProperty("num")
    private Integer quantity;
    
    private Long subtotal;
    private Integer stock;
    private Boolean selected;
    
    /** 作品是否锁定（结算中） */
    private Boolean locked;
    
    /** 卖家ID */
    private Long sellerId;
    
    /** 卖家名称 */
    private String sellerName;
    
    /** 卖家头像 */
    private String sellerAvatar;
    
    /** 所属发布者ID */
    private Long publisherId;
    
    /** 发布者名称 */
    private String publisherName;
    
    /** 发布者头像 */
    private String publisherAvatar;
    
    /** 艺荐官ID */
    private Long promoterId;
    
    /** 艺荐官名称 */
    private String promoterName;
    
    /** 艺荐官头像 */
    private String promoterAvatar;
}
