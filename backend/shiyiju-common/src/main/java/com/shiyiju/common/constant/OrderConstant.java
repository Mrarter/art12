package com.shiyiju.common.constant;

/**
 * 订单常量
 */
public class OrderConstant {

    /** 订单状态 - 待付款 */
    public static final Integer STATUS_PENDING_PAYMENT = 1;

    /** 订单状态 - 已取消 */
    public static final Integer STATUS_CANCELLED = 0;

    /** 订单状态 - 已付款（待发货） */
    public static final Integer STATUS_PAID = 2;

    /** 订单状态 - 已发货 */
    public static final Integer STATUS_SHIPPED = 3;

    /** 订单状态 - 已收货（待评价） */
    public static final Integer STATUS_RECEIVED = 4;

    /** 订单状态 - 已完成 */
    public static final Integer STATUS_COMPLETED = 5;

    /** 订单状态 - 退款中 */
    public static final Integer STATUS_REFUNDING = 6;

    /** 订单状态 - 已退款 */
    public static final Integer STATUS_REFUNDED = 7;

    /** 订单来源 - 立即购买 */
    public static final Integer SOURCE_DIRECT = 1;

    /** 订单来源 - 购物车 */
    public static final Integer SOURCE_CART = 2;

    /** 订单来源 - 拍卖成交 */
    public static final Integer SOURCE_AUCTION = 3;
}
