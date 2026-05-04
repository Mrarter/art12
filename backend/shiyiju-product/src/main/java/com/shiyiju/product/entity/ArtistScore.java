package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("artist_score")
public class ArtistScore implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("artist_id")
    private Long artistId;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("sales_score")
    private Integer salesScore;

    @TableField("influence_score")
    private Integer influenceScore;

    @TableField("activity_score")
    private Integer activityScore;

    @TableField("quality_score")
    private Integer qualityScore;

    @TableField("review_score")
    private Integer reviewScore;

    @TableField("academic_score")
    private Integer academicScore;

    @TableField("internet_score")
    private Integer internetScore;

    private String level;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
