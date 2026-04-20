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
    private Long price; // 当前价格（已计算增长率）
    private Long originalPrice; // 原始价格
    private Long currentPrice; // 实时计算价格（包含最新热度因素）
    private Integer stock;
    private String statusText;
    private Integer status;
    private Integer weight; // 权重
    private Integer ownershipType; // 作品类型: 1-原创, 2-收藏
    private String ownershipTypeText; // 作品类型文本
    private String artworkCode; // 作品编号
    private Integer viewCount;
    private BigDecimal priceRise;
    private Integer favoriteCount;
    private Integer saleCount;
    private Boolean isFavorited;
    private String createTime;
    // 分销相关
    private Boolean distributionEnabled;
    private Integer commissionRate;
    private Integer distributionOrders;
    private Long distributionEarnings;
    private Integer distributionUsers;
}
