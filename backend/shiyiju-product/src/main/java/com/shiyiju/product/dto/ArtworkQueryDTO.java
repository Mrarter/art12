package com.shiyiju.product.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ArtworkQueryDTO implements Serializable {
    private Integer page = 1;
    private Integer pageSize = 20;
    private Long id; // 作品ID搜索
    private String title; // 作品名称搜索
    private String authorName; // 艺术家名称搜索
    private Long categoryId;
    private String keyword;
    private String sortBy; // price, createTime, saleCount
    private String sortOrder; // asc, desc
    private String sort; // price_asc, price_desc, time, new (for new API)
    private Long minPrice;
    private Long maxPrice;
    private Integer yearFrom;
    private Integer yearTo;
    private String region; // 艺术家地区
    private Integer holdDuration; // 持有时长
    private Integer status; // 状态筛选，不传则查所有状态
}
