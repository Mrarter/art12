package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 作品表 */
@Data
@TableName("artworks")
public class Artwork implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long authorId;
    private Long categoryId;
    private String artType;
    private String medium;
    private String size;
    private Integer year;
    private String edition;
    private String description;
    private String coverImage;
    private String images;
    private Integer source;
    private Long holderId;
    private LocalDateTime holderSince;
    private Long price;
    private Long originalPrice;
    private Integer stock;
    private Integer status;
    private BigDecimal priceRise;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer saleCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
