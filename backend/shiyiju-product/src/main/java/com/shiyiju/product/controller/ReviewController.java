package com.shiyiju.product.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.product.dto.ReviewCreateDTO;
import com.shiyiju.product.service.ReviewService;
import com.shiyiju.product.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 作品评价控制器
 */
@Slf4j
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 创建评价 (POST /review)
     */
    @PostMapping
    public Result<Long> createReview(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody ReviewCreateDTO dto
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        Long reviewId = reviewService.createReview(userId, dto);
        return Result.success(reviewId);
    }

    /**
     * 获取作品评价列表 (GET /review/artwork/{artworkId})
     */
    @GetMapping("/artwork/{artworkId}")
    public Result<PageResult<ReviewVO>> getArtworkReviews(
            @PathVariable Long artworkId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(reviewService.getArtworkReviews(artworkId, page, pageSize));
    }

    /**
     * 获取作品评分统计 (GET /review/artwork/{artworkId}/stats)
     */
    @GetMapping("/artwork/{artworkId}/stats")
    public Result<Map<String, Object>> getArtworkRatingStats(@PathVariable Long artworkId) {
        return Result.success(reviewService.getArtworkRatingStats(artworkId));
    }

    /**
     * 获取我的评价列表 (GET /review/my)
     */
    @GetMapping("/my")
    public Result<PageResult<ReviewVO>> getMyReviews(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(reviewService.getUserReviews(userId, page, pageSize));
    }

    /**
     * 获取订单评价列表 (GET /review/order/{orderId})
     */
    @GetMapping("/order/{orderId}")
    public Result<List<ReviewVO>> getOrderReviews(@PathVariable Long orderId) {
        return Result.success(reviewService.getOrderReviews(orderId));
    }

    /**
     * 获取评价详情 (GET /review/{id})
     */
    @GetMapping("/{id}")
    public Result<ReviewVO> getReviewDetail(@PathVariable Long id) {
        return Result.success(reviewService.getReviewDetail(id));
    }

    /**
     * 删除评价 (DELETE /review/{id})
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteReview(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        reviewService.deleteReview(id, userId);
        return Result.success();
    }

    /**
     * 商家回复评价 (POST /review/{id}/reply)
     */
    @PostMapping("/{id}/reply")
    public Result<Void> replyToReview(
            @RequestHeader(value = "X-User-Id", required = false) Long sellerId,
            @PathVariable Long id,
            @RequestBody Map<String, String> params
    ) {
        if (sellerId == null) {
            return Result.fail(401, "请先登录");
        }
        String content = params.get("content");
        if (content == null || content.isEmpty()) {
            return Result.fail(400, "回复内容不能为空");
        }
        reviewService.replyToReview(id, sellerId, content);
        return Result.success();
    }
}
