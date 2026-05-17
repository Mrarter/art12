package com.shiyiju.common.event;

/**
 * 金融事件类型 - 所有资金变动统一枚举
 *
 * 每个事件最终由 FinanceEventHandler 处理并调用 WalletService。
 */
public enum FinanceEventType {

    /** 普通入账（艺术家收益） */
    ARTIST_INCOME,
    /** 转售卖家收入 */
    SELLER_INCOME,
    /** 平台服务费入账 */
    PLATFORM_FEE,
    /** 佣金结算 */
    COMMISSION_SETTLE,
    /** 转售标记已支付 */
    RESALE_MARK_PAID,
    /** 退款 - 扣回艺术家收益 */
    REFUND_ARTIST,
    /** 退款 - 扣回卖家收入 */
    REFUND_SELLER,
    /** 退款 - 扣回平台服务费 */
    REFUND_PLATFORM,
    /** 转售退款回滚 */
    RESALE_ROLLBACK
}
