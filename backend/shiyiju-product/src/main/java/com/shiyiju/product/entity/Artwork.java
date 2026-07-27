package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 作品表 */
@Data
@TableName("artwork")
public class Artwork implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 作品ID（与id相同） */
    @TableField("artwork_id")
    private Long artworkId;
    
    /** 作品标准化UID (19位: ART + 日期 + 序列 + 随机码) */
    @TableField("artwork_code")
    private String artworkUid;
    
    private String title;
    private Long authorId;
    
    /** 作者用户UID */
    @TableField("author_uid")
    private String authorUid;
    
    private String authorName;
    private String authorBadge;
    private String authorAvatar;
    private String authorBio;    // 艺术家简介
    private String authorPhone;   // 艺术家电话
    private Long categoryId;
    private String artType;
    private String medium;
    private String size;
    private Integer year;
    private String edition;
    private String description;
    @TableField("cover")
    private String cover;  // 优先使用 cover 字段
    @TableField("cover_image")
    private String coverImage;  // 备选字段
    private String images;
    private Integer source;
    private Long holderId;
    private LocalDateTime holderSince;
    /** 当前价格（元） */
    private BigDecimal price;
    /** 原始价格（元） */
    private BigDecimal originalPrice;
    /** 运费（元） */
    private BigDecimal freight;
    private Integer stock;
    private Integer status;
    private Integer weight; // 权重，数值越大越靠前
    private Integer ownershipType; // 作品类型: 1-原创, 2-收藏
    private String artworkCode; // 作品编号，如 yh202604200001
    private BigDecimal priceRise;
    private Integer viewCount;
    @TableField("daily_view_count")
    private Integer dailyViewCount;
    @TableField("daily_like_count")
    private Integer dailyLikeCount;
    private Integer favoriteCount;
    @TableField(exist = false)  // 数据库中无此列
    private Integer saleCount;
    // 评价相关
    private Integer rating; // 平均评分（1-5星）
    @TableField("review_count")
    private Integer reviewCount; // 评价数量
    // 分销相关
    @TableField("distribution_enabled")
    private Boolean distributionEnabled;
    @TableField("commission_rate")
    private Integer commissionRate;
    @TableField("distribution_orders")
    private Integer distributionOrders;
    @TableField("distribution_earnings")
    private Long distributionEarnings;
    @TableField("distribution_users")
    private Integer distributionUsers;
    // 单个作品价格增长配置
    @TableField(exist = false)  // 数据库中无此列
    private Boolean customPriceGrowthEnabled; // 是否启用自定义价格增长
    @TableField(exist = false)  // 数据库中无此列
    private Boolean platformPriceGrowthEnabled; // 是否启用平台涨价策略
    @TableField(exist = false)  // 数据库中无此列
    private BigDecimal customBaseDailyRate; // 自定义基础日增长率
    @TableField(exist = false)  // 数据库中无此列
    private BigDecimal customMatureDailyRate; // 自定义成熟期日增长率
    @TableField(exist = false)  // 数据库中无此列
    private Integer customMatureDays; // 自定义成熟期天数
    @TableField(exist = false)  // 数据库中无此列
    private BigDecimal customViewRate; // 自定义浏览量加成系数
    @TableField(exist = false)  // 数据库中无此列
    private BigDecimal customFavoriteRate; // 自定义收藏量加成系数
    @TableField(exist = false)  // 数据库中无此列
    private BigDecimal customMaxGrowthMultiple; // 自定义最大涨幅倍数
    /** 内容指纹 SHA256(title + authorId + yyyyMMdd)，用于幂等去重 */
    @TableField("content_fingerprint")
    private String contentFingerprint;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(exist = false)  // 数据库中无此列
    private Integer deleted;
}
