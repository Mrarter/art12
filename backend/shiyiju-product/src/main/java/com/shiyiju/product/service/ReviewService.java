package com.shiyiju.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.product.dto.ReviewCreateDTO;
import com.shiyiju.product.entity.Artwork;
import com.shiyiju.product.entity.Review;
import com.shiyiju.product.entity.ReviewReply;
import com.shiyiju.product.mapper.ArtworkMapper;
import com.shiyiju.product.mapper.ReviewMapper;
import com.shiyiju.product.mapper.ReviewReplyMapper;
import com.shiyiju.product.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作品评价服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final ArtworkMapper artworkMapper;
    // 注意：UserMapper 在 shiyiju-user 模块中，如需获取用户信息请通过Feign调用

    /**
     * 创建评价
     */
    @Transactional
    public Long createReview(Long userId, ReviewCreateDTO dto) {
        // 校验作品是否存在
        Artwork artwork = artworkMapper.selectById(dto.getArtworkId());
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // 检查是否已评价
        Review existingReview = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .eq(Review::getOrderItemId, dto.getOrderItemId())
        );
        if (existingReview != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该订单项已评价");
        }

        // 获取用户信息
        String userNickname = "用户" + userId;
        String userAvatar = null;

        // 创建评价
        Review review = new Review();
        review.setOrderId(dto.getOrderId());
        review.setOrderItemId(dto.getOrderItemId());
        review.setArtworkId(dto.getArtworkId());
        review.setUserId(userId);
        review.setUserNickname(userNickname);
        review.setUserAvatar(userAvatar);
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setImages(dto.getImages());
        review.setQualityRating(dto.getQualityRating() != null ? dto.getQualityRating() : dto.getRating());
        review.setLogisticsRating(dto.getLogisticsRating() != null ? dto.getLogisticsRating() : dto.getRating());
        review.setServiceRating(dto.getServiceRating() != null ? dto.getServiceRating() : dto.getRating());
        review.setIsAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : 0);
        review.setStatus(1); // 直接通过审核
        review.setLikeCount(0);
        review.setReplyCount(0);
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());

        reviewMapper.insert(review);

        // 更新作品的评分统计
        updateArtworkRatingStats(dto.getArtworkId());

        log.info("用户{}创建评价成功，评价ID: {}", userId, review.getId());
        return review.getId();
    }

    /**
     * 获取作品评价列表
     */
    public PageResult<ReviewVO> getArtworkReviews(Long artworkId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getArtworkId, artworkId);
        wrapper.eq(Review::getStatus, 1); // 只显示已审核通过的评价
        wrapper.orderByDesc(Review::getCreateTime);

        Page<Review> pageResult = new Page<>(page, pageSize);
        Page<Review> result = reviewMapper.selectPage(pageResult, wrapper);

        List<ReviewVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), page, pageSize, voList);
    }

    /**
     * 获取用户评价列表
     */
    public PageResult<ReviewVO> getUserReviews(Long userId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId);
        wrapper.eq(Review::getStatus, 1);
        wrapper.orderByDesc(Review::getCreateTime);

        Page<Review> pageResult = new Page<>(page, pageSize);
        Page<Review> result = reviewMapper.selectPage(pageResult, wrapper);

        List<ReviewVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), page, pageSize, voList);
    }

    /**
     * 获取订单的评价列表
     */
    public List<ReviewVO> getOrderReviews(Long orderId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .eq(Review::getStatus, 1)
        );

        return reviews.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取评价详情
     */
    public ReviewVO getReviewDetail(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评价不存在");
        }
        return convertToVO(review);
    }

    /**
     * 删除评价
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权删除该评价");
        }

        reviewMapper.deleteById(reviewId);

        // 更新作品评分统计
        updateArtworkRatingStats(review.getArtworkId());

        log.info("用户{}删除评价{}", userId, reviewId);
    }

    /**
     * 获取作品评分统计
     */
    public Map<String, Object> getArtworkRatingStats(Long artworkId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getArtworkId, artworkId);
        wrapper.eq(Review::getStatus, 1);

        List<Review> reviews = reviewMapper.selectList(wrapper);

        Map<String, Object> stats = new java.util.HashMap<>();
        if (reviews.isEmpty()) {
            stats.put("totalCount", 0);
            stats.put("averageRating", 0.0);
            stats.put("rating1Count", 0);
            stats.put("rating2Count", 0);
            stats.put("rating3Count", 0);
            stats.put("rating4Count", 0);
            stats.put("rating5Count", 0);
        } else {
            stats.put("totalCount", reviews.size());
            double avgRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            stats.put("averageRating", Math.round(avgRating * 10) / 10.0);

            stats.put("rating1Count", (int) reviews.stream().filter(r -> r.getRating() == 1).count());
            stats.put("rating2Count", (int) reviews.stream().filter(r -> r.getRating() == 2).count());
            stats.put("rating3Count", (int) reviews.stream().filter(r -> r.getRating() == 3).count());
            stats.put("rating4Count", (int) reviews.stream().filter(r -> r.getRating() == 4).count());
            stats.put("rating5Count", (int) reviews.stream().filter(r -> r.getRating() == 5).count());
        }

        return stats;
    }

    /**
     * 商家回复评价
     */
    @Transactional
    public void replyToReview(Long reviewId, Long sellerId, String content) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评价不存在");
        }

        // 检查是否已回复
        ReviewReply existingReply = reviewReplyMapper.selectOne(
                new LambdaQueryWrapper<ReviewReply>()
                        .eq(ReviewReply::getReviewId, reviewId)
                        .eq(ReviewReply::getReplyType, "seller")
        );
        if (existingReply != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该评价已回复");
        }

        // 创建回复
        ReviewReply reply = new ReviewReply();
        reply.setReviewId(reviewId);
        reply.setUserId(sellerId);
        reply.setUserNickname("商家");
        reply.setContent(content);
        reply.setReplyType("seller");
        reply.setStatus(1);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());

        reviewReplyMapper.insert(reply);

        // 更新评价的回复数
        review.setReplyCount(review.getReplyCount() + 1);
        reviewMapper.updateById(review);

        log.info("商家{}回复评价{}", sellerId, reviewId);
    }

    /**
     * 更新作品评分统计
     */
    private void updateArtworkRatingStats(Long artworkId) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) return;

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getArtworkId, artworkId);
        wrapper.eq(Review::getStatus, 1);

        List<Review> reviews = reviewMapper.selectList(wrapper);

        if (reviews.isEmpty()) {
            artwork.setRating(null);
        } else {
            double avgRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            artwork.setRating((int) Math.round(avgRating));
        }

        artwork.setReviewCount(reviews.size());
        artwork.setUpdateTime(LocalDateTime.now());
        artworkMapper.updateById(artwork);
    }

    /**
     * 转换为VO
     */
    private ReviewVO convertToVO(Review review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setArtworkId(review.getArtworkId());
        vo.setUserId(review.getUserId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        
        // 处理图片
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            vo.setImages(Arrays.asList(review.getImages().split(",")));
        }

        vo.setQualityRating(review.getQualityRating());
        vo.setLogisticsRating(review.getLogisticsRating());
        vo.setServiceRating(review.getServiceRating());
        vo.setIsAnonymous(review.getIsAnonymous());
        vo.setLikeCount(review.getLikeCount());
        vo.setReplyCount(review.getReplyCount());
        vo.setCreateTime(review.getCreateTime() != null ? review.getCreateTime().toString() : null);

        // 匿名处理
        if (review.getIsAnonymous() != null && review.getIsAnonymous() == 1) {
            vo.setUserNickname("匿名用户");
            vo.setUserAvatar(null);
        } else {
            vo.setUserNickname(review.getUserNickname());
            vo.setUserAvatar(review.getUserAvatar());
        }

        // 获取作品信息
        Artwork artwork = artworkMapper.selectById(review.getArtworkId());
        if (artwork != null) {
            vo.setArtworkTitle(artwork.getTitle());
            vo.setArtworkCover(artwork.getCoverImage());
        }

        // 获取商家回复
        ReviewReply sellerReply = reviewReplyMapper.selectOne(
                new LambdaQueryWrapper<ReviewReply>()
                        .eq(ReviewReply::getReviewId, review.getId())
                        .eq(ReviewReply::getReplyType, "seller")
        );
        if (sellerReply != null) {
            vo.setSellerReply(sellerReply.getContent());
            vo.setSellerReplyTime(sellerReply.getCreateTime() != null ? 
                    sellerReply.getCreateTime().toString() : null);
        }

        return vo;
    }
}
