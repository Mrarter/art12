package com.shiyiju.product.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ArtworkUpdateDTO {
    private Long id;
    private Long authorId;
    private String title;
    private String authorName;
    private Long categoryId;
    private String cover;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String description;
    private Integer status;
    private Integer weight; // 权重
    // 作品类型: 1-原创, 2-收藏
    private Integer ownershipType;
    private String artworkCode; // 作品编号
    // 画种、尺寸、年份
    private String artType;
    private String size;
    private Integer year;
    // 每日展示热度配置
    private Integer dailyViewCount;
    private Integer dailyLikeCount;
    // 分销相关
    private Boolean distributionEnabled;
    private Integer commissionRate;
}
