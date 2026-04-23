package com.shiyiju.common.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    /** 艺术家名称 */
    @JsonProperty("authorName")
    private String authorName;
    
    private String authorAvatar;
    private String authorBadge;
    private Long categoryId;
    
    @JsonProperty("category")
    private String categoryName;
    
    private String artType;
    
    /** 材质 */
    private String material;
    
    private String size;
    
    /** 创作年份 */
    private Integer year;
    private String edition;
    private String description;
    
    /** 封面图 */
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
    
    /** 价格涨幅 */
    @JsonProperty("priceRise")
    private BigDecimal priceRise;
    
    private Integer favoriteCount;
    private Integer saleCount;
    
    /** 是否已收藏 */
    @JsonProperty("isFavorite")
    private Boolean isFavorited;
    private String createTime;
    
    /** 是否新品（创建时间在30天内） */
    @JsonProperty("isNew")
    private Boolean isNew;
    
    /** 是否热门（销量>0或收藏数>5） */
    @JsonProperty("isHot")
    private Boolean isHot;
    
    /** 艺术家身份类型：artist/collector */
    private String authorIdentity;
    
    /** 艺术家电话 */
    private String authorPhone;
    
    /** 艺术家简介 */
    private String authorBio;
    
    /** 是否已关注该艺术家 */
    private Boolean isFollowing;
    
    /** 持有时长（天） */
    private Integer holdDuration;
    
    // 分销相关
    private Boolean distributionEnabled;
    private Integer commissionRate;
    private Integer distributionOrders;
    private Long distributionEarnings;
    private Integer distributionUsers;
}
