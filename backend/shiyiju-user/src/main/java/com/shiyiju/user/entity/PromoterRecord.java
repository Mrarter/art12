package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 艺荐官记录表
 */
@Data
@TableName("promoter_record")
public class PromoterRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 邀请码 */
    private String inviteCode;

    /** 上级艺荐官ID */
    private Long parentId;

    /** 等级: 1-普通, 2-白银, 3-黄金, 4-钻石 */
    private Integer level;

    /** 团队人数 */
    private Integer teamSize;

    /** 累计订单数 */
    private Integer totalOrders;

    /** 累计销售额 */
    private BigDecimal totalSales;

    /** 状态: 0-禁用, 1-正常 */
    private Integer status;

    /** 签约时间 */
    private LocalDateTime signTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
