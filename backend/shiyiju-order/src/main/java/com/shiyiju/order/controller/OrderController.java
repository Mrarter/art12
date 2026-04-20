package com.shiyiju.order.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.order.dto.CreateOrderDTO;
import com.shiyiju.common.entity.Address;
import com.shiyiju.order.entity.Order;
import com.shiyiju.order.service.OrderService;
import com.shiyiju.order.vo.CartVO;
import com.shiyiju.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ==================== 购物车模块 ====================

    /**
     * 获取购物车列表 (GET /cart/list)
     */
    @GetMapping("/cart/list")
    public Result<List<CartVO>> getCartList(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(orderService.getCartList(userId));
    }

    /**
     * 添加到购物车 (POST /cart/add)
     */
    @PostMapping("/cart/add")
    public Result<Void> addToCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        Long artworkId = Long.valueOf(params.get("artworkId").toString());
        Integer quantity = params.get("quantity") != null ? Integer.valueOf(params.get("quantity").toString()) : 1;
        orderService.addToCart(userId, artworkId, quantity);
        return Result.success();
    }

    /**
     * 更新购物车数量 (PUT /cart/update)
     */
    @PutMapping("/cart/update")
    public Result<Void> updateCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        Long cartId = Long.valueOf(params.get("cartId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        orderService.updateCartQuantity(userId, cartId, quantity);
        return Result.success();
    }

    /**
     * 删除购物车项 (DELETE /cart/delete)
     */
    @DeleteMapping("/cart/delete")
    public Result<Void> deleteCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody List<Long> cartIds
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        orderService.removeFromCart(userId, cartIds);
        return Result.success();
    }

    /**
     * 锁定购物车项（结算前）(POST /cart/lock)
     */
    @PostMapping("/cart/lock")
    public Result<Map<String, Object>> lockCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody List<Long> cartIds
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(orderService.lockCartItems(userId, cartIds));
    }

    /**
     * 解锁购物车项 (POST /cart/unlock)
     */
    @PostMapping("/cart/unlock")
    public Result<Void> unlockCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody List<Long> cartIds
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        orderService.unlockCartItems(userId, cartIds);
        return Result.success();
    }

    // ==================== 订单模块 ====================

    /**
     * 创建订单（从购物车）(POST /orders/create)
     */
    @PostMapping("/orders/create")
    public Result<Order> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody CreateOrderDTO dto
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(orderService.createOrderFromCart(userId, dto));
    }

    /**
     * 直接购买 (POST /orders/direct)
     */
    @PostMapping("/orders/direct")
    public Result<Order> directBuy(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody CreateOrderDTO dto
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(orderService.createDirectOrder(userId, dto));
    }

    /**
     * 获取订单列表 (GET /orders)
     */
    @GetMapping("/orders")
    public Result<PageResult<OrderVO>> getOrderList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(orderService.getOrderList(userId, status, page, pageSize));
    }

    /**
     * 获取订单详情 (GET /orders/{id})
     */
    @GetMapping("/orders/{id}")
    public Result<OrderVO> getOrderDetail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(orderService.getOrderDetail(id, userId));
    }

    /**
     * 取消订单 (PUT /orders/{id}/cancel)
     */
    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancelOrder(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        orderService.cancelOrder(id, userId);
        return Result.success();
    }

    /**
     * 确认收货 (PUT /orders/{id}/confirm)
     */
    @PutMapping("/orders/{id}/confirm")
    public Result<Void> confirmReceive(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        orderService.confirmReceive(id, userId);
        return Result.success();
    }

    /**
     * 申请售后 (POST /orders/{id}/refund)
     */
    @PostMapping("/orders/{id}/refund")
    public Result<Void> applyRefund(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        orderService.applyRefund(id, userId, params.get("reason"));
        return Result.success();
    }

    // ==================== 支付模块 ====================

    /**
     * 微信支付统一下单 (POST /pay/unified-order)
     * 返回支付二维码链接或预支付ID
     */
    @PostMapping("/pay/unified-order")
    public Result<String> unifiedOrder(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        String openId = params.get("openId") != null ? params.get("openId").toString() : null;
        return Result.success(orderService.unifiedOrder(
                Long.valueOf(params.get("orderId").toString()), 
                userId,
                openId));
    }

    /**
     * 微信支付 - 获取JSAPI支付参数 (POST /pay/jsapi-params)
     * 返回小程序调起支付的必要参数
     */
    @PostMapping("/pay/jsapi-params")
    public Result<Map<String, Object>> getJsApiParams(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        String openId = params.get("openId") != null ? params.get("openId").toString() : null;
        if (openId == null || openId.isEmpty()) {
            return Result.fail(400, "缺少openId参数");
        }
        return Result.success(orderService.unifiedOrderWithParams(
                Long.valueOf(params.get("orderId").toString()), 
                userId,
                openId));
    }

    /**
     * 微信支付 - 查询订单状态 (GET /pay/query/{orderId})
     */
    @GetMapping("/pay/query/{orderId}")
    public Result<Map<String, String>> queryPayStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long orderId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        Order order = orderService.getOrderById(orderId, userId);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success(orderService.queryPayStatus(order.getOrderNo()));
    }

    // ==================== 个人中心 - 地址管理 ====================

    /**
     * 获取收货地址列表 (GET /user/addresses)
     */
    @GetMapping("/user/addresses")
    public Result<List<Address>> getAddressList(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(orderService.getAddressList(userId));
    }

    /**
     * 添加收货地址 (POST /user/addresses)
     */
    @PostMapping("/user/addresses")
    public Result<Void> addAddress(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Address address
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        orderService.addAddress(userId, address);
        return Result.success();
    }

    /**
     * 更新收货地址 (PUT /user/addresses/{id})
     */
    @PutMapping("/user/addresses/{id}")
    public Result<Void> updateAddress(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @RequestBody Address address
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        orderService.updateAddress(id, userId, address);
        return Result.success();
    }

    /**
     * 删除收货地址 (DELETE /user/addresses/{id})
     */
    @DeleteMapping("/user/addresses/{id}")
    public Result<Void> deleteAddress(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        orderService.deleteAddress(id, userId);
        return Result.success();
    }

    // ==================== 兼容旧路径的接口 ====================

    /** 获取订单列表 (旧路径) */
    @GetMapping("/list")
    public Result<PageResult<OrderVO>> getOrderListLegacy(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        return Result.success(orderService.getOrderList(userId, status, page, pageSize));
    }

    /** 获取订单详情 (旧路径) */
    @GetMapping("/detail/{orderId}")
    public Result<OrderVO> getOrderDetailLegacy(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId
    ) {
        return Result.success(orderService.getOrderDetail(orderId, userId));
    }

    /** 取消订单 (旧路径) */
    @PutMapping("/cancel/{orderId}")
    public Result<Void> cancelOrderLegacy(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId
    ) {
        orderService.cancelOrder(orderId, userId);
        return Result.success();
    }

    /** 获取收货地址列表 (旧路径) */
    @GetMapping("/address/list")
    public Result<List<Address>> getAddressListLegacy(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.getAddressList(userId));
    }

    /** 添加收货地址 (旧路径) */
    @PostMapping("/address/add")
    public Result<Void> addAddressLegacy(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Address address
    ) {
        orderService.addAddress(userId, address);
        return Result.success();
    }

    /** 删除收货地址 (旧路径) */
    @DeleteMapping("/address/delete/{addressId}")
    public Result<Void> deleteAddressLegacy(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long addressId
    ) {
        orderService.deleteAddress(addressId, userId);
        return Result.success();
    }
}
