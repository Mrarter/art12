package com.shiyiju.common.constant;

/**
 * 订单常量
 */
public class OrderConstant {

    /** 订单状态 - 已取消 */
    public static final String STATUS_CANCELLED = "CANCELLED";

    /** 订单状态 - 待付款 */
    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";

    /** 订单状态 - 已付款（待发货） */
    public static final String STATUS_PAID = "PAID";

    /** 订单状态 - 已发货 */
    public static final String STATUS_SHIPPED = "SHIPPED";

    /** 订单状态 - 已收货 */
    public static final String STATUS_RECEIVED = "RECEIVED";

    /** 订单状态 - 已完成 */
    public static final String STATUS_COMPLETED = "COMPLETED";

    /** 订单状态 - 退款中 */
    public static final String STATUS_REFUNDING = "REFUNDING";

    /** 订单状态 - 已退款 */
    public static final String STATUS_REFUNDED = "REFUNDED";

    /** 订单来源 - 立即购买 */
    public static final String SOURCE_DIRECT = "DIRECT";

    /** 订单来源 - 购物车 */
    public static final String SOURCE_CART = "CART";

    /** 订单来源 - 拍卖成交 */
    public static final String SOURCE_AUCTION = "AUCTION";
}
