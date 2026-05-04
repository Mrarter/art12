package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("artist_identity")
public class ArtistIdentity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("artist_id")
    private Long artistId;

    @TableField("school_name")
    private String schoolName;

    private String degree;

    @TableField("academic_title")
    private String academicTitle;

    @TableField("association_name")
    private String associationName;

    private String exhibitions;

    private String awards;

    @TableField("social_platform")
    private String socialPlatform;

    @TableField("social_account_url")
    private String socialAccountUrl;

    @TableField("follower_count")
    private Integer followerCount;

    @TableField("content_type")
    private String contentType;

    @TableField("content_quality_score")
    private Integer contentQualityScore;

    @TableField("conversion_score")
    private Integer conversionScore;

    private Integer verified;

    @TableField("audit_status")
    private String auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
