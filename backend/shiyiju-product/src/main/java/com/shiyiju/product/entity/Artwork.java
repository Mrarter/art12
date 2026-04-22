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
    private String title;
    private Long authorId;
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
    private String coverImage;
    private String images;
    private Integer source;
    private Long holderId;
    private LocalDateTime holderSince;
    private Long price;
    private Long originalPrice;
    private Integer stock;
    private Integer status;
    private Integer weight; // 权重，数值越大越靠前
    private Integer ownershipType; // 作品类型: 1-原创, 2-收藏
    private String artworkCode; // 作品编号，如 yh202604200001
    private BigDecimal priceRise;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer saleCount;
    // 评价相关
    private Integer rating; // 平均评分（1-5星）
    private Integer reviewCount; // 评价数量
    // 分销相关
    private Boolean distributionEnabled;
    private Integer commissionRate;
    private Integer distributionOrders;
    private Long distributionEarnings;
    private Integer distributionUsers;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
