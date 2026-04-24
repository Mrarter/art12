package com.shiyiju.admin.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作品实体（复用业务表）
 */
@Data
public class Artwork {
    private Long id;
    private String title;
    private Long authorId;
    private String authorName;
    private Long categoryId;
    private String categoryName;
    private String coverImage;
    private BigDecimal price;
    private Integer stock;
    private Integer salesCount;
    private Integer status;
    private LocalDateTime createTime;
}
