package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转售记录表
 */
@Data
@TableName("resale_record")
public class ResaleRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作品ID */
    private Long artworkId;

    /** 卖家用户ID */
    private Long sellerUserId;

    /** 买家用户ID */
    private Long buyerUserId;

    /** 作品UID（展示用，非表字段） */
    @TableField(exist = false)
    private String artworkUid;

    /** 作品标题（展示用，非表字段） */
    @TableField(exist = false)
    private String artworkTitle;

    /** 作品封面（展示用，非表字段） */
    @TableField(exist = false)
    private String artworkCoverImage;

    /** 艺术家名称（展示用，非表字段） */
    @TableField(exist = false)
    private String artistName;

    /** 作品门类（展示用，非表字段） */
    @TableField(exist = false)
    private String categoryName;

    /** 作品类型（展示用，非表字段） */
    @TableField(exist = false)
    private String artworkArtType;

    /** 作品材质（展示用，非表字段） */
    @TableField(exist = false)
    private String artworkMedium;

    /** 作品尺寸（展示用，非表字段） */
    @TableField(exist = false)
    private String artworkSize;

    /** 创作年份（展示用，非表字段） */
    @TableField(exist = false)
    private Integer artworkYear;

    /** 当前作品评估价格（展示用，非表字段） */
    @TableField(exist = false)
    private BigDecimal artworkCurrentPrice;

    /** 当前持有者买入价（展示用，非表字段） */
    @TableField(exist = false)
    private BigDecimal holderBuyPrice;

    /** 建议转售价格下限（展示用，非表字段） */
    @TableField(exist = false)
    private BigDecimal suggestedMinPrice;

    /** 建议转售价格上限（展示用，非表字段） */
    @TableField(exist = false)
    private BigDecimal suggestedMaxPrice;

    /** 卖家用户UID（展示用，非表字段） */
    @TableField(exist = false)
    private String sellerUid;

    /** 买家用户UID（展示用，非表字段） */
    @TableField(exist = false)
    private String buyerUid;

    /** 来源订单ID（首次出售或上次转售的订单） */
    private Long sourceOrderId;

    /** 转售价格 */
    private BigDecimal resalePrice;

    /** 艺术家持续收益 */
    private BigDecimal artistIncome;

    /** 平台服务费 */
    private BigDecimal platformFee;

    /** 卖家实际到账收入 */
    private BigDecimal sellerIncome;

    /** 艺术家收益比例（展示用，非表字段） */
    @TableField(exist = false)
    private BigDecimal artistIncomeRate;

    /** 平台服务费比例（展示用，非表字段） */
    @TableField(exist = false)
    private BigDecimal platformFeeRate;

    /** 是否启用平台评估与热度涨价机制（展示用，非表字段） */
    @TableField(exist = false)
    private Boolean platformPricingEnabled;

    /** 平台调控后的建议转售价（展示用，非表字段） */
    @TableField(exist = false)
    private BigDecimal platformManagedPrice;

    /** 状态: pending/paid/completed/cancel */
    private String status;

    /** 交易编号（用于幂等控制） */
    private String tradeNo;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
