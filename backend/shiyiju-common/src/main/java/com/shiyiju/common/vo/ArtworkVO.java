package com.shiyiju.common.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 艺术品VO - 跨模块共享
 */
@Data
public class ArtworkVO implements Serializable {
    private Long id;
    private String title;
    private Long authorId;
    
    @JsonProperty("authorName")
    private String authorName;
    
    private String authorAvatar;
    private String authorBadge;
    private Long categoryId;
    
    @JsonProperty("category")
    private String categoryName;
    
    private String artType;
    private String material;
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
    
    /** 当前价格（元，DECIMAL） */
    private BigDecimal price;
    /** 原始价格（元） */
    private BigDecimal originalPrice;
    /** 运费（元） */
    private BigDecimal freight;
    /** 兼容订单确认页的运费字段 */
    private BigDecimal postageFee;
    /** 实时计算价格 */
    private BigDecimal currentPrice;
    
    private Integer stock;
    private String statusText;
    private Integer status;
    private Integer weight;
    private Integer ownershipType;
    private String ownershipTypeText;
    private String artworkCode;
    private Integer viewCount;
    private Integer realViewCount;
    private Integer dailyViewCount;
    private Integer displayViewCount;
    
    @JsonProperty("priceRise")
    private BigDecimal priceRise;
    
    private Integer favoriteCount;
    private Integer realFavoriteCount;
    private Integer dailyLikeCount;
    private Integer displayLikeCount;
    private Integer likeCount;
    private Integer saleCount;
    
    @JsonProperty("isFavorite")
    private Boolean isFavorited;
    private String createTime;
    
    @JsonProperty("isNew")
    private Boolean isNew;
    
    @JsonProperty("isHot")
    private Boolean isHot;
    
    private String authorIdentity;
    private String authorPhone;
    private String authorBio;
    private Boolean isFollowing;
    private Integer holdDuration;
    private String displayArtworkId;
    private String displayAuthorId;
    private String authorUid;
    
    // 分销相关
    private Boolean distributionEnabled;
    private Integer commissionRate;
    private Integer distributionOrders;
    private Long distributionEarnings;
    private Integer distributionUsers;
    
    // 价格增长配置
    private Boolean platformPriceGrowthEnabled;
    private Boolean customPriceGrowthEnabled;
    private BigDecimal customBaseDailyRate;
    private BigDecimal customMatureDailyRate;
    private Integer customMatureDays;
    private BigDecimal customViewRate;
    private BigDecimal customFavoriteRate;
    private BigDecimal customMaxGrowthMultiple;
    /** 明日预计涨价最低（元） */
    private BigDecimal tomorrowIncreaseMin;
    /** 明日预计涨价最高（元） */
    private BigDecimal tomorrowIncreaseMax;

    /** 当前生效中的转售挂单 */
    private Map<String, Object> resaleListing;
}
