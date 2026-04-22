package com.shiyiju.product.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价VO
 */
@Data
public class ReviewVO {

    private Long id;

    private Long orderId;

    private Long artworkId;

    private String artworkTitle;

    private String artworkCover;

    private Long userId;

    private String userNickname;

    private String userAvatar;

    private Integer rating;

    private String content;

    private List<String> images;

    private Integer qualityRating;

    private Integer logisticsRating;

    private Integer serviceRating;

    private Integer isAnonymous;

    private Integer likeCount;

    private Integer replyCount;

    private String createTime;

    private String statusText;

    /**
     * 商家回复
     */
    private String sellerReply;

    private String sellerReplyTime;
}
