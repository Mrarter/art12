package com.shiyiju.order.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class OrderItemVO implements Serializable {
    private Long id;
    
    /** 商品ID - 前端期望 goodsId */
    @JsonProperty("goodsId")
    private Long artworkId;
    
    /** 商品名称 - 前端期望 goodsName */
    @JsonProperty("goodsName")
    private String title;
    
    /** 商品图片 - 前端期望 goodsImage */
    @JsonProperty("goodsImage")
    private String coverImage;
    
    private String authorName;
    private Long price;
    
    /** 商品数量 - 前端期望 count */
    @JsonProperty("count")
    private Integer quantity;
    
    private Long subtotal;
    
    /** 规格名称 - 前端期望 specName */
    private String specName;
    
    /** 艺术家名称 */
    private String artistName;
    
    /** 画种 */
    private String artType;
    
    /** 尺寸 */
    private String size;
}
