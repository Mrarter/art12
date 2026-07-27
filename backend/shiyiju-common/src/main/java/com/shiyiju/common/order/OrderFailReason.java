package com.shiyiju.common.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单失败原因枚举
 * 用于归类订单创建失败的具体原因，便于前端展示友好错误信息
 */
@Getter
@AllArgsConstructor
public enum OrderFailReason {

    /** 库存不足 */
    STOCK_INSUFFICIENT("STOCK_INSUFFICIENT", "库存不足", "该作品库存不足，请重新选择"),

    /** 库存锁定失败（并发） */
    INVENTORY_LOCK_FAILED("INVENTORY_LOCK_FAILED", "库存锁定失败", "系统繁忙，请稍后重试"),

    /** 商品已下架 */
    PRODUCT_OFF_SHELF("PRODUCT_OFF_SHELF", "商品已下架", "该作品已下架，请浏览其他作品"),

    /** 商品已售罄 */
    PRODUCT_SOLD_OUT("PRODUCT_SOLD_OUT", "商品已售罄", "该作品已售罄，请浏览其他作品"),

    /** 价格已变动 */
    PRICE_CHANGED("PRICE_CHANGED", "价格已变动", "订单金额已变更，请重新确认"),

    /** 并发冲突（乐观锁失败） */
    CONCURRENT_CONFLICT("CONCURRENT_CONFLICT", "并发冲突", "系统繁忙，请稍后重试"),

    /** 数据库异常 */
    DATABASE_ERROR("DATABASE_ERROR", "数据库异常", "系统繁忙，请稍后重试"),

    /** 支付超时 */
    PAYMENT_TIMEOUT("PAYMENT_TIMEOUT", "支付超时", "支付超时，请重新下单"),

    /** 地址无效 */
    ADDRESS_INVALID("ADDRESS_INVALID", "地址无效", "收货地址无效，请重新选择"),

    /** 用户信息异常 */
    USER_INVALID("USER_INVALID", "用户信息异常", "用户信息错误，请重新登录"),

    /** 参数错误 */
    PARAM_INVALID("PARAM_INVALID", "参数错误", "请求参数错误，请重试"),

    /** 网络中断/超时 */
    NETWORK_ERROR("NETWORK_ERROR", "网络异常", "网络连接异常，请检查网络后重试"),

    /** 分布式锁争用失败 */
    LOCK_ACQUIRE_FAILED("LOCK_ACQUIRE_FAILED", "系统繁忙", "当前操作人数较多，请稍后重试"),

    /** 服务熔断 */
    CIRCUIT_BREAKER("CIRCUIT_BREAKER", "服务暂不可用", "服务暂不可用，请稍后重试"),

    /** 未知内部错误 */
    INTERNAL_ERROR("INTERNAL_ERROR", "系统繁忙", "系统繁忙，请稍后重试");

    private final String code;
    private final String shortMessage;
    private final String userMessage;
}
