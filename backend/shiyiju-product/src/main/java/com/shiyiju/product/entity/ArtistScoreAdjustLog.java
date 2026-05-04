package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("artist_score_adjust_log")
public class ArtistScoreAdjustLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("artist_id")
    private Long artistId;

    @TableField("old_score")
    private Integer oldScore;

    @TableField("adjust_score")
    private Integer adjustScore;

    @TableField("new_score")
    private Integer newScore;

    private String reason;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
