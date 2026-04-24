package com.shiyiju.admin.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 艺荐官记录实体
 */
@Data
public class PromoterRecord {
    private Long id;
    private Long userId;
    private String nickname;
    private String inviteCode;
    private Long parentId;
    private String parentNickname;
    private Integer level;
    private Integer teamSize;
    private Integer totalOrders;
    private BigDecimal totalSales;
    private Integer status;
    private LocalDateTime signTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
