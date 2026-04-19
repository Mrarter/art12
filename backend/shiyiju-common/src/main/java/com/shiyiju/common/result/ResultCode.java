package com.shiyiju.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    FAIL(400, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // 服务端错误 5xx
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务错误 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USER_DISABLED(1003, "用户已被禁用"),
    PASSWORD_ERROR(1004, "密码错误"),
    TOKEN_INVALID(1005, "Token无效"),
    TOKEN_EXPIRED(1006, "Token已过期"),

    // 认证相关 11xx
    ARTIST_NOT_CERTIFIED(1101, "未完成艺术家认证"),
    AGENT_NOT_CERTIFIED(1102, "未完成经纪人认证"),
    PROMOTER_NOT_OPENED(1103, "未开通艺荐官"),
    PROMOTER_AGREEMENT_NOT_SIGNED(1104, "未签署分销协议"),

    // 商品相关 12xx
    PRODUCT_NOT_FOUND(1201, "作品不存在"),
    PRODUCT_OFF_SHELF(1202, "作品已下架"),
    PRODUCT_SOLD_OUT(1203, "作品已售罄"),
    STOCK_NOT_ENOUGH(1204, "库存不足"),

    // 订单相关 13xx
    ORDER_NOT_FOUND(1301, "订单不存在"),
    ORDER_CANNOT_CANCEL(1302, "订单不可取消"),
    ORDER_CANNOT_CONFIRM(1303, "订单不可确认收货"),
    ORDER_PRICE_CHANGED(1304, "订单价格已变动"),

    // 支付相关 14xx
    PAYMENT_FAILED(1401, "支付失败"),
    PAYMENT_TIMEOUT(1402, "支付超时"),
    PAYMENT_AMOUNT_MISMATCH(1403, "支付金额不匹配"),

    // 拍卖相关 15xx
    AUCTION_NOT_STARTED(1501, "拍卖未开始"),
    AUCTION_ENDED(1502, "拍卖已结束"),
    AUCTION_DEPOSIT_REQUIRED(1503, "需缴纳保证金"),
    AUCTION_BID_TOO_LOW(1504, "出价金额过低"),
    AUCTION_BID_FAILED(1505, "出价失败"),

    // 分销相关 16xx
    INVITE_CODE_INVALID(1601, "邀请码无效"),
    INVITE_CODE_USED(1602, "邀请码已使用"),
    COMMISSION_WITHDRAW_INSUFFICIENT(1603, "可提现佣金不足"),
    PROMOTER_LEVEL_INSUFFICIENT(1604, "艺荐官等级不足"),

    // 文件相关 17xx
    FILE_UPLOAD_FAILED(1701, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(1702, "文件类型不允许"),
    FILE_SIZE_EXCEED(1703, "文件大小超出限制");

    private final Integer code;
    private final String message;
}
