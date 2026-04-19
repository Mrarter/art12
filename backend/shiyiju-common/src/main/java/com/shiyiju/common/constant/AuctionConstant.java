package com.shiyiju.common.constant;

/**
 * 拍卖常量
 */
public class AuctionConstant {

    /** 拍卖专场状态 - 预告中 */
    public static final Integer SESSION_STATUS_COMING = 1;

    /** 拍卖专场状态 - 进行中 */
    public static final Integer SESSION_STATUS_ONGOING = 2;

    /** 拍卖专场状态 - 已结束 */
    public static final Integer SESSION_STATUS_ENDED = 3;

    /** 拍品状态 - 待拍卖 */
    public static final Integer LOT_STATUS_PENDING = 0;

    /** 拍品状态 - 拍卖中 */
    public static final Integer LOT_STATUS_ONGOING = 1;

    /** 拍品状态 - 已成交 */
    public static final Integer LOT_STATUS_SOLD = 2;

    /** 拍品状态 - 流拍 */
    public static final Integer LOT_STATUS_UNSOLD = 3;

    /** 保证金状态 - 待支付 */
    public static final Integer DEPOSIT_UNPAID = 0;

    /** 保证金状态 - 已支付 */
    public static final Integer DEPOSIT_PAID = 1;

    /** 保证金状态 - 已退款 */
    public static final Integer DEPOSIT_REFUNDED = 2;

    /** 保证金状态 - 已抵扣 */
    public static final Integer DEPOSIT_DEDUCTED = 3;
}
