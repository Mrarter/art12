package com.shiyiju.common.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 艺术品VO - 跨模块共享
 */
@Data
public class ArtworkVO implements Serializable {
    private Long id;
    private String title;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String authorBadge;
    private Long categoryId;
    private String categoryName;
    private String artType;
    private String medium;
    private String size;
    private Integer year;
    private String edition;
    private String description;
    private String coverImage;
    private List<String> images;
    private Integer source;
    private String sourceText;
    private Long holderId;
    private String holderName;
    private String holderSince;
    private Long price;
    private Long originalPrice;
    private Integer stock;
    private String statusText;
    private BigDecimal priceRise;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer saleCount;
    private Boolean isFavorited;
    private String createTime;
}
