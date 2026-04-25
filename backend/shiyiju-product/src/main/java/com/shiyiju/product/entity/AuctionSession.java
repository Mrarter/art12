package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 拍卖专场表实体
 * 对应表: auction_session
 */
@Data
@TableName("auction_session")
public class AuctionSession implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 专场标准化编号 (19位: SES + 日期 + 序列 + 随机码) */
    @TableField("session_code")
    private String sessionCode;
    
    private String title;
    private String coverImage;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status; // 状态: 0-预告, 1-进行中, 2-已结束
    private Integer totalLots; // 总拍品数
    private Integer totalBids; // 总出价次数
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
