package com.shiyiju.order.exception;

import com.shiyiju.common.result.Result;
import com.shiyiju.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单模块本地异常处理器
 * 捕获 OrderFailCreateException 返回包含 retryable 标志的详细错误信息
 *
 * @Order(Ordered.LOWEST_PRECEDENCE - 1) 确保在全局处理器之前执行
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class OrderExceptionHandler {

    @ExceptionHandler(OrderFailCreateException.class)
    public Result<Map<String, Object>> handleOrderFailCreate(OrderFailCreateException e) {
        log.warn("订单创建失败 - reason={}, retryable={}, code={}, message={}",
                e.getFailReason().getCode(), e.getRetryable(), e.getCode(), e.getMessage());

        Map<String, Object> extra = new HashMap<>();
        extra.put("failReason", e.getFailReason().getCode());
        extra.put("retryable", e.getRetryable());
        extra.put("shortMessage", e.getFailReason().getShortMessage());

        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 兜底：所有未处理的订单异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOrderException(Exception e) {
        log.error("订单系统异常", e);
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}
