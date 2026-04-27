package com.shiyiju.auction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("auction_session")
public class AuctionSession implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String coverImage;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Integer totalLots;
    private Integer totalBids;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
