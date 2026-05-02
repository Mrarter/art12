package com.yixiangji.artist.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArtistIdentity {
    private Long id;
    private Long artistId;
    private String schoolName;
    private String degree;
    private String academicTitle;
    private String associationName;
    private String exhibitions;
    private String awards;
    private String socialPlatform;
    private String socialAccountUrl;
    private Integer followerCount;
    private String contentType;
    private Integer contentQualityScore;
    private Integer conversionScore;
    private Integer verified;
    private String auditStatus;
    private String auditRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
