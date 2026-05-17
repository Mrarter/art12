package com.shiyiju.order.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderItemVO implements Serializable {
    private Long id;
    
    @JsonProperty("goodsId")
    private Long artworkId;
    
    @JsonProperty("goodsName")
    private String title;
    
    @JsonProperty("goodsImage")
    private String coverImage;
    
    private String authorName;
    /** 价格（元） */
    private BigDecimal price;
    
    @JsonProperty("count")
    private Integer quantity;
    
    /** 小计（元） */
    private BigDecimal subtotal;
    
    @JsonProperty("specName")
    private String specName;
    
    private String artistName;
    private String artType;
    private String size;
}
