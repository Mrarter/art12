package com.shiyiju.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.constant.OrderConstant;
import com.shiyiju.common.constant.ProductConstant;
import com.shiyiju.common.entity.Address;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.mapper.AddressMapper;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.order.dto.CreateOrderDTO;
import com.shiyiju.order.entity.*;
import com.shiyiju.order.mapper.*;
import com.shiyiju.order.vo.CartVO;
import com.shiyiju.order.vo.OrderItemVO;
import com.shiyiju.order.vo.OrderVO;
import com.shiyiju.product.entity.Artwork;
import com.shiyiju.product.mapper.ArtworkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final AddressMapper addressMapper;
    private final ArtworkMapper artworkMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 获取购物车列表 */
    public List<CartVO> getCartList(Long userId) {
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getCreateTime)
        );

        return carts.stream().map(cart -> {
            Artwork artwork = artworkMapper.selectById(cart.getArtworkId());
            if (artwork == null) return null;
            
            CartVO vo = new CartVO();
            vo.setId(cart.getId());
            vo.setArtworkId(artwork.getId());
            vo.setTitle(artwork.getTitle());
            vo.setCoverImage(artwork.getCoverImage());
            vo.setSize(artwork.getSize());
            vo.setPrice(artwork.getPrice());
            vo.setQuantity(cart.getQuantity());
            vo.setSubtotal(artwork.getPrice() * cart.getQuantity());
            vo.setStock(artwork.getStock());
            vo.setSelected(false);
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** 添加到购物车 */
    @Transactional
    public void addToCart(Long userId, Long artworkId, Integer quantity) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (artwork.getStatus() != ProductConstant.STATUS_ON_SALE) {
            throw new BusinessException(ResultCode.PRODUCT_OFF_SHELF);
        }
        if (artwork.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }

        Cart existing = cartMapper.selectOne(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(Cart::getArtworkId, artworkId)
        );

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartMapper.updateById(existing);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setArtworkId(artworkId);
            cart.setQuantity(quantity);
            cart.setCreateTime(LocalDateTime.now());
            cartMapper.insert(cart);
        }
    }

    /** 从购物车移除 */
    @Transactional
    public void removeFromCart(Long userId, List<Long> cartIds) {
        cartMapper.delete(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .in(Cart::getId, cartIds)
        );
    }

    /** 更新购物车数量 */
    @Transactional
    public void updateCartQuantity(Long userId, Long cartId, Integer quantity) {
        Cart cart = cartMapper.selectOne(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getId, cartId)
                        .eq(Cart::getUserId, userId)
        );
        if (cart == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "购物车记录不存在");
        }
        
        Artwork artwork = artworkMapper.selectById(cart.getArtworkId());
        if (artwork != null && artwork.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
        
        cart.setQuantity(quantity);
        cart.setUpdateTime(LocalDateTime.now());
        cartMapper.updateById(cart);
    }

    /** 锁定购物车项（结算前）- 使用 Redis 防止超卖 */
    public Map<String, Object> lockCartItems(Long userId, List<Long> cartIds) {
        String lockKey = "cart:lock:" + userId;
        List<Map<String, Object>> lockedItems = new ArrayList<>();
        List<Long> failedItems = new ArrayList<>();
        
        for (Long cartId : cartIds) {
            Cart cart = cartMapper.selectOne(
                    new LambdaQueryWrapper<Cart>()
                            .eq(Cart::getId, cartId)
                            .eq(Cart::getUserId, userId)
            );
            
            if (cart == null) {
                failedItems.add(cartId);
                continue;
            }
            
            Artwork artwork = artworkMapper.selectById(cart.getArtworkId());
            if (artwork == null) {
                failedItems.add(cartId);
                continue;
            }
            
            // 检查 Redis 中是否已被锁定
            String itemLockKey = "cart:item:lock:" + cartId;
            Boolean alreadyLocked = redisTemplate.hasKey(itemLockKey);
            
            if (Boolean.TRUE.equals(alreadyLocked)) {
                failedItems.add(cartId);
                continue;
            }
            
            // 尝试获取 Redis 分布式锁
            Boolean lockAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(itemLockKey, userId.toString(), 15, TimeUnit.MINUTES);
            
            if (Boolean.TRUE.equals(lockAcquired)) {
                Map<String, Object> item = new HashMap<>();
                item.put("cartId", cartId);
                item.put("artworkId", artwork.getId());
                item.put("title", artwork.getTitle());
                item.put("price", artwork.getPrice());
                item.put("quantity", cart.getQuantity());
                item.put("stock", artwork.getStock());
                item.put("lockExpired", false);
                lockedItems.add(item);
            } else {
                failedItems.add(cartId);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("lockedItems", lockedItems);
        result.put("failedItems", failedItems);
        result.put("success", failedItems.isEmpty());
        
        return result;
    }

    /** 解锁购物车项 */
    public void unlockCartItems(Long userId, List<Long> cartIds) {
        for (Long cartId : cartIds) {
            String itemLockKey = "cart:item:lock:" + cartId;
            redisTemplate.delete(itemLockKey);
        }
    }

    /** 从购物车创建订单 (POST /orders/cart-create) */
    @Transactional
    public Order createOrderFromCart(Long userId, CreateOrderDTO dto) {
        // 验证锁定状态
        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
            for (Long cartId : dto.getCartIds()) {
                String itemLockKey = "cart:item:lock:" + cartId;
                Object lockedUserId = redisTemplate.opsForValue().get(itemLockKey);
                if (lockedUserId == null || !userId.toString().equals(lockedUserId.toString())) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "购物车项未锁定或已过期，请重新选择");
                }
            }
        }
        return createOrder(userId, dto);
    }

    /** 直接购买 (POST /orders/direct) */
    @Transactional
    public Order createDirectOrder(Long userId, CreateOrderDTO dto) {
        return createOrder(userId, dto);
    }

    /** 申请售后 (POST /orders/{id}/refund) */
    @Transactional
    public void applyRefund(Long orderId, Long userId, String reason) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderConstant.STATUS_COMPLETED && 
            order.getStatus() != OrderConstant.STATUS_SHIPPED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不允许申请售后");
        }

        order.setStatus(OrderConstant.STATUS_REFUNDING);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // TODO: 发送退款申请消息
    }

    /** 更新收货地址 */
    @Transactional
    public void updateAddress(Long addressId, Long userId, Address addressUpdate) {
        Address address = addressMapper.selectOne(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getId, addressId)
                        .eq(Address::getUserId, userId)
        );
        if (address == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收货地址不存在");
        }

        if (addressUpdate.getReceiverName() != null) {
            address.setReceiverName(addressUpdate.getReceiverName());
        }
        if (addressUpdate.getPhone() != null) {
            address.setReceiverPhone(addressUpdate.getPhone());
        }
        if (addressUpdate.getProvince() != null) {
            address.setProvince(addressUpdate.getProvince());
        }
        if (addressUpdate.getCity() != null) {
            address.setCity(addressUpdate.getCity());
        }
        if (addressUpdate.getDistrict() != null) {
            address.setDistrict(addressUpdate.getDistrict());
        }
        if (addressUpdate.getDetailAddress() != null) {
            address.setDetailAddress(addressUpdate.getDetailAddress());
        }
        if (addressUpdate.getIsDefault() != null) {
            if (addressUpdate.getIsDefault() == 1) {
                // 清除其他默认地址
                addressMapper.update(null,
                        new LambdaQueryWrapper<Address>()
                                .eq(Address::getUserId, userId)
                                .eq(Address::getIsDefault, 1)
                );
            }
            address.setIsDefault(addressUpdate.getIsDefault());
        }
        address.setUpdateTime(LocalDateTime.now());
        addressMapper.updateById(address);
    }

    /** 创建订单 */
    @Transactional
    public Order createOrder(Long userId, CreateOrderDTO dto) {
        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收货地址不存在");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        long totalAmount = 0;

        // 从购物车创建
        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
            List<Cart> carts = cartMapper.selectList(
                    new LambdaQueryWrapper<Cart>()
                            .eq(Cart::getUserId, userId)
                            .in(Cart::getId, dto.getCartIds())
            );

            for (Cart cart : carts) {
                Artwork artwork = artworkMapper.selectById(cart.getArtworkId());
                if (artwork == null) continue;

                if (artwork.getStock() < cart.getQuantity()) {
                    throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, "作品【" + artwork.getTitle() + "】库存不足");
                }

                OrderItem item = new OrderItem();
                item.setArtworkId(artwork.getId());
                item.setTitle(artwork.getTitle());
                item.setCoverImage(artwork.getCoverImage());
                item.setAuthorName("艺术家");
                item.setPrice(artwork.getPrice());
                item.setQuantity(cart.getQuantity());
                item.setSubtotal(artwork.getPrice() * cart.getQuantity());
                orderItems.add(item);
                totalAmount += artwork.getPrice() * cart.getQuantity();
            }

            // 清空购物车
            cartMapper.delete(
                    new LambdaQueryWrapper<Cart>()
                            .eq(Cart::getUserId, userId)
                            .in(Cart::getId, dto.getCartIds())
            );
        }

        // 直接购买
        if (dto.getArtworkId() != null) {
            Artwork artwork = artworkMapper.selectById(dto.getArtworkId());
            if (artwork == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
            }
            int qty = dto.getQuantity() != null ? dto.getQuantity() : 1;
            if (artwork.getStock() < qty) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }

            OrderItem item = new OrderItem();
            item.setArtworkId(artwork.getId());
            item.setTitle(artwork.getTitle());
            item.setCoverImage(artwork.getCoverImage());
            item.setAuthorName("艺术家");
            item.setPrice(artwork.getPrice());
            item.setQuantity(qty);
            item.setSubtotal(artwork.getPrice() * qty);
            orderItems.add(item);
            totalAmount += artwork.getPrice() * qty;
        }

        if (orderItems.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单项为空");
        }

        // 生成订单号
        String orderNo = "SYJ" + LocalDateTime.now().format(ORDER_NO_FORMAT) + 
                         String.format("%04d", userId % 10000);

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(0L);
        order.setPayAmount(totalAmount);
        order.setCommissionAmount(0L);
        order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        order.setRemark(dto.getRemark());
        order.setSource(dto.getCartIds() != null ? OrderConstant.SOURCE_CART : OrderConstant.SOURCE_DIRECT);
        order.setStatus(OrderConstant.STATUS_PENDING_PAYMENT);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 保存订单项
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            item.setCreateTime(LocalDateTime.now());
            orderItemMapper.insert(item);

            // 扣减库存
            Artwork artwork = artworkMapper.selectById(item.getArtworkId());
            artwork.setStock(artwork.getStock() - item.getQuantity());
            if (artwork.getStock() <= 0) {
                artwork.setStatus(ProductConstant.STATUS_SOLD_OUT);
            }
            artworkMapper.updateById(artwork);
        }

        return order;
    }

    /** 获取订单列表 */
    public PageResult<OrderVO> getOrderList(Long userId, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> result = orderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<OrderVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), page, pageSize, voList);
    }

    /** 获取订单详情 */
    public OrderVO getOrderDetail(Long orderId, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return convertToVO(order);
    }

    /** 取消订单 */
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderConstant.STATUS_PENDING_PAYMENT) {
            throw new BusinessException(ResultCode.ORDER_CANNOT_CANCEL);
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        for (OrderItem item : items) {
            Artwork artwork = artworkMapper.selectById(item.getArtworkId());
            artwork.setStock(artwork.getStock() + item.getQuantity());
            if (artwork.getStatus() == ProductConstant.STATUS_SOLD_OUT) {
                artwork.setStatus(ProductConstant.STATUS_ON_SALE);
            }
            artworkMapper.updateById(artwork);
        }

        order.setStatus(OrderConstant.STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 确认收货 */
    @Transactional
    public void confirmReceive(Long orderId, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderConstant.STATUS_SHIPPED) {
            throw new BusinessException(ResultCode.ORDER_CANNOT_CONFIRM);
        }

        order.setStatus(OrderConstant.STATUS_COMPLETED);
        order.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // TODO: 解锁佣金（如果是分销订单）
    }

    /** 微信支付统一下单 */
    public String unifiedOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderConstant.STATUS_PENDING_PAYMENT) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态不允许支付");
        }

        // TODO: 调用微信支付统一下单接口
        // 返回预支付交易会话标识
        String prepayId = "prepay_" + System.currentTimeMillis();
        
        // 存入Redis，设置支付过期时间（30分钟）
        redisTemplate.opsForValue().set("pay:order:" + orderId, prepayId, 30, TimeUnit.MINUTES);
        
        return prepayId;
    }

    /** 支付回调处理 */
    @Transactional
    public void handlePayCallback(String orderNo, String transactionId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        if (order == null) return;

        order.setStatus(OrderConstant.STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // TODO: 发送订单状态消息通知
    }

    /** 获取收货地址列表 */
    public List<Address> getAddressList(Long userId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getCreateTime)
        );
    }

    /** 添加收货地址 */
    @Transactional
    public void addAddress(Long userId, Address address) {
        address.setUserId(userId);
        if (address.getIsDefault() == null || address.getIsDefault() == 1) {
            // 清除其他默认地址
            addressMapper.update(null,
                    new LambdaQueryWrapper<Address>()
                            .eq(Address::getUserId, userId)
                            .eq(Address::getIsDefault, 1)
            );
            address.setIsDefault(1);
        }
        address.setCreateTime(LocalDateTime.now());
        addressMapper.insert(address);
    }

    /** 删除收货地址 */
    public void deleteAddress(Long addressId, Long userId) {
        addressMapper.delete(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getId, addressId)
                        .eq(Address::getUserId, userId)
        );
    }

    /** 设置默认地址 */
    @Transactional
    public void setDefaultAddress(Long addressId, Long userId) {
        addressMapper.update(null,
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .eq(Address::getIsDefault, 1)
        );
        Address address = addressMapper.selectOne(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getId, addressId)
                        .eq(Address::getUserId, userId)
        );
        if (address != null) {
            address.setIsDefault(1);
            addressMapper.updateById(address);
        }
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setSource(order.getSource());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime() != null ? order.getPayTime().toString() : null);
        vo.setShipTime(order.getShipTime() != null ? order.getShipTime().toString() : null);
        vo.setReceiveTime(order.getReceiveTime() != null ? order.getReceiveTime().toString() : null);
        vo.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : null);

        vo.setSourceText(order.getSource() == 1 ? "立即购买" : order.getSource() == 2 ? "购物车" : "拍卖");
        vo.setStatusText(getStatusText(order.getStatus()));

        // 加载订单项
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setArtworkId(item.getArtworkId());
            itemVO.setTitle(item.getTitle());
            itemVO.setCoverImage(item.getCoverImage());
            itemVO.setAuthorName(item.getAuthorName());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getSubtotal());
            return itemVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "已取消";
            case 1 -> "待付款";
            case 2 -> "已付款";
            case 3 -> "已发货";
            case 4 -> "已收货";
            case 5 -> "已完成";
            case 6 -> "退款中";
            case 7 -> "已退款";
            default -> "未知";
        };
    }
}
