package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金记录表实体
 * 对应表: commission_log
 */
@Data
@TableName("commission_log")
public class CommissionLog implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 佣金标准化编号 (19位: COM + 日期 + 序列 + 随机码) */
    @TableField("commission_code")
    private String commissionCode;
    
    private Long userId; // 艺荐官ID
    private Long orderId; // 关联订单ID
    private String orderNo; // 订单号
    private Long artworkId; // 作品ID
    private Long buyerId; // 购买用户ID
    private BigDecimal orderAmount; // 订单金额
    private BigDecimal commissionRate; // 佣金比例
    private BigDecimal commissionAmount; // 佣金金额
    private Integer commissionType; // 类型: 1-直接推广, 2-团队奖励
    private Integer level; // 等级: 1-一级, 2-二级
    private Integer status; // 状态: 0-待结算, 1-已结算, 2-已失效
    private LocalDateTime settleTime; // 结算时间
    private String remark; // 备注
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
