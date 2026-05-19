package com.shiyiju.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户互动数据校验结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteractionStatsVO {

    /** 用户 ID */
    private Long userId;

    /** 真实关注数（user_follows 表统计） */
    private Integer followingCount;

    /** 真实收藏数（artwork_favorites 表统计，仅统计作品仍存在的记录） */
    private Integer favoriteCount;

    /** 真实点赞数（post_likes 表统计，仅统计帖子仍存在的记录） */
    private Integer likeCount;

    /** 用户表中记录的关注数（用于对比校验） */
    private Integer recordedFollowingCount;

    /** 数据校验状态 */
    private VerificationStatus verificationStatus;

    /** 校验详情 */
    private VerificationDetail verificationDetail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationStatus {
        /** 是否通过校验（所有数据一致） */
        private Boolean passed;
        /** 校验等级：GREEN（一致）/ YELLOW（轻微差异）/ RED（严重差异） */
        private String level;
        /** 校验结论描述 */
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationDetail {
        /** 关注数是否存在差异 */
        private Boolean followingDiscrepancy;
        /** 收藏数是否存在异常 */
        private Boolean favoriteAnomaly;
        /** 点赞数是否存在异常 */
        private Boolean likeAnomaly;
        /** 具体差异说明列表 */
        private List<String> issues;
    }
}
