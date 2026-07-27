package com.shiyiju.order.exception;

import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.order.OrderFailReason;
import com.shiyiju.common.result.ResultCode;
import lombok.Getter;

/**
 * 创建订单失败专用异常
 * 携带失败原因和错误码（带友好用户提示），供全局异常处理器统一返回
 *
 * 继承 BusinessException 以复用 GlobalExceptionHandler 中的处理逻辑
 */
@Getter
public class OrderFailCreateException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /** 失败原因枚举 */
    private final OrderFailReason failReason;

    /** 是否可重试 */
    private final Boolean retryable;

    public OrderFailCreateException(OrderFailReason failReason) {
        super(resolveErrorCode(failReason), failReason.getUserMessage());
        this.failReason = failReason;
        this.retryable = isRetryableError(failReason);
    }

    public OrderFailCreateException(OrderFailReason failReason, String detailMessage) {
        super(resolveErrorCode(failReason), detailMessage != null ? detailMessage : failReason.getUserMessage());
        this.failReason = failReason;
        this.retryable = isRetryableError(failReason);
    }

    /**
     * 根据失败原因映射到前端错误码
     */
    private static Integer resolveErrorCode(OrderFailReason reason) {
        switch (reason) {
            case STOCK_INSUFFICIENT:      return ResultCode.STOCK_NOT_ENOUGH.getCode();
            case PRODUCT_OFF_SHELF:       return ResultCode.PRODUCT_OFF_SHELF.getCode();
            case PRODUCT_SOLD_OUT:        return ResultCode.PRODUCT_SOLD_OUT.getCode();
            case PRICE_CHANGED:           return ResultCode.ORDER_PRICE_CHANGED.getCode();
            case PAYMENT_TIMEOUT:         return ResultCode.PAYMENT_TIMEOUT.getCode();
            case ADDRESS_INVALID:         return ResultCode.PARAM_ERROR.getCode();
            case USER_INVALID:            return ResultCode.USER_NOT_FOUND.getCode();
            case PARAM_INVALID:           return ResultCode.PARAM_ERROR.getCode();
            default:                      return ResultCode.FAIL.getCode();
        }
    }

    /**
     * 判断该错误是否可重试
     * 幂等性校验失败、参数错误、权限不足等不可重试
     */
    public static boolean isRetryableError(OrderFailReason reason) {
        switch (reason) {
            case STOCK_INSUFFICIENT:
            case PRODUCT_OFF_SHELF:
            case PRODUCT_SOLD_OUT:
            case PRICE_CHANGED:
            case ADDRESS_INVALID:
            case USER_INVALID:
            case PARAM_INVALID:
                return false;
            default:
                return true;
        }
    }
}
