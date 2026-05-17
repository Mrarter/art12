package com.shiyiju.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单失败记录 - 用于订单创建失败时的日志记录、自动回滚与重试
 */
@Data
@TableName("order_fail_record")
public class OrderFailRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 失败时已生成的订单号（可能为空，如库存校验阶段就已失败） */
    private String orderNo;

    /** 买家用户ID */
    private Long userId;

    /** 作品ID */
    private Long artworkId;

    /** 转售记录ID */
    private Long resaleId;

    /** 购物车ID列表（逗号分隔） */
    private String cartIds;

    /** 订单来源: DIRECT / CART / RESALE / AUCTION */
    private String source;

    /** 失败原因枚举值 */
    private String failReason;

    /** 详细错误信息 */
    private String failMessage;

    /** 请求参数JSON */
    private String requestParams;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetries;

    /**
     * 重试状态:
     * 0-未重试
     * 1-重试中
     * 2-重试成功
     * 3-重试失败（已放弃）
     */
    private Integer retryStatus;

    /** 是否已补偿回滚: 0-未补偿 1-已补偿 */
    private Integer compensated;

    /** 补偿回滚时间 */
    private LocalDateTime compensateAt;

    /** 下次重试时间 */
    private LocalDateTime nextRetryAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
