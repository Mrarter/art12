package com.shiyiju.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 金融事件 - 所有资金变动的统一载体
 *
 * 替代 OrderService 中直接 HTTP 调用 WalletRestClient / CommissionRestClient / ResaleRestClient。
 * 事件发布后由 FinanceEventHandler 异步处理，保证最终一致性。
 *
 * 使用方式：
 *   financeEventPublisher.publish(FinanceEvent.builder()
 *       .type(FinanceEventType.ARTIST_INCOME)
 *       .userId(artistId)
 *       .amount(new BigDecimal("500.00"))
 *       .relatedId(orderId)
 *       .relatedType("order")
 *       .remark("作品销售收益: 订单号")
 *       .build());
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型 */
    private FinanceEventType type;

    /** 目标用户ID（收益方/扣款方） */
    private Long userId;

    /** 金额（正数=入账，负数=出账） */
    private BigDecimal amount;

    /** 关联业务ID（订单ID/转售ID） */
    private Long relatedId;

    /** 关联业务类型（order/resale/commission） */
    private String relatedType;

    /** 备注 */
    private String remark;

    // ===== 转售专用字段 =====

    /** 转售ID（RESALE_MARK_PAID / RESALE_ROLLBACK 时使用） */
    private Long resaleId;

    /** 买家用户ID（RESALE_MARK_PAID 时使用） */
    private Long buyerUserId;

    /** 作品ID */
    private Long artworkId;

    /** 订单号 */
    private String orderNo;

    // ===== 平台专用字段 =====

    /** 平台钱包用户ID（PLATFORM_FEE 时使用） */
    private Long platformWalletUserId;

    /** 艺术家收益（REFUND_SELLER 时需要） */
    private BigDecimal artistIncome;

    /** 平台费用（REFUND_SELLER 时需要） */
    private BigDecimal platformFee;

    /** 卖家收入（REFUND_SELLER 时需要） */
    private BigDecimal sellerIncome;
}
