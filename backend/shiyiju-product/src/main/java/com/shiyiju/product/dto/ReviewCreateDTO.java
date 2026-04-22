package com.shiyiju.product.dto;

import lombok.Data;

/**
 * 评价创建DTO
 */
@Data
public class ReviewCreateDTO {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单项ID
     */
    private Long orderItemId;

    /**
     * 作品ID
     */
    private Long artworkId;

    /**
     * 评分（1-5星）
     */
    private Integer rating;

    /**
     * 评价内容
     */
    private String content;

    /**
     * 评价图片（多个用逗号分隔）
     */
    private String images;

    /**
     * 作品状态评分
     */
    private Integer qualityRating;

    /**
     * 物流服务评分
     */
    private Integer logisticsRating;

    /**
     * 服务态度评分
     */
    private Integer serviceRating;

    /**
     * 是否匿名（0-否 1-是）
     */
    private Integer isAnonymous;
}
