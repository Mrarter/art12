package com.yixiangji.artist.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArtistScore {
    private Long id;
    private Long artistId;
    private Integer totalScore;
    private Integer salesScore;
    private Integer influenceScore;
    private Integer activityScore;
    private Integer qualityScore;
    private Integer reviewScore;
    private Integer academicScore;
    private Integer internetScore;
    private String level;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
