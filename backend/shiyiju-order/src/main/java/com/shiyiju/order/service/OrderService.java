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
import com.shiyiju.common.service.WxPayService;
import com.shiyiju.order.dto.CreateOrderDTO;
import com.shiyiju.order.entity.*;
import com.shiyiju.order.mapper.*;
import com.shiyiju.order.vo.CartVO;
import com.shiyiju.order.vo.OrderItemVO;
import com.shiyiju.order.vo.OrderVO;
import com.shiyiju.common.entity.Artwork;
import com.shiyiju.common.mapper.ArtworkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final WxPayService wxPayService;

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    // 佣金比例
    private static final BigDecimal DIRECT_COMMISSION_RATE = new BigDecimal("0.05"); // 一级佣金 5%
    private static final BigDecimal TEAM_COMMISSION_RATE = new BigDecimal("0.02"); // 二级团队奖励 2%

    /** 获取购物车列表 */
    public List<CartVO> getCartList(Long userId) {
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getCreatedAt)
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
            cart.setCreatedAt(LocalDateTime.now());
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
        cart.setUpdatedAt(LocalDateTime.now());
        cartMapper.updateById(cart);
    }

    /** 锁定购物车项（结算前）- 使用 Redis 防止超卖 */
    public Map<String, Object> lockCartItems(Long userId, List<Long> cartIds) {
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

    /** 从购物车创建订单 */
    @Transactional
    public Order createOrderFromCart(Long userId, CreateOrderDTO dto) {
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

    /** 直接购买 */
    @Transactional
    public Order createDirectOrder(Long userId, CreateOrderDTO dto) {
        return createOrder(userId, dto);
    }

    /** 创建订单 */
    @Transactional
    public Order createOrder(Long userId, CreateOrderDTO dto) {
        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收货地址不存在");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

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

                OrderItem item = createOrderItem(artwork, cart.getQuantity());
                orderItems.add(item);
                totalAmount = totalAmount.add(BigDecimal.valueOf(artwork.getPrice()).multiply(BigDecimal.valueOf(cart.getQuantity())));
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

            OrderItem item = createOrderItem(artwork, qty);
            orderItems.add(item);
            totalAmount = totalAmount.add(BigDecimal.valueOf(artwork.getPrice()).multiply(BigDecimal.valueOf(qty)));
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
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setCommissionAmount(BigDecimal.ZERO);
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

    private OrderItem createOrderItem(Artwork artwork, int quantity) {
        OrderItem item = new OrderItem();
        item.setArtworkId(artwork.getId());
        item.setArtistId(artwork.getAuthorId());  // 使用 authorId
        item.setItemType("ARTWORK");
        item.setTitle(artwork.getTitle());
        item.setCoverImage(artwork.getCoverImage());
        item.setPrice(artwork.getPrice());
        item.setQuantity(quantity);
        item.setSubtotal(artwork.getPrice() * quantity);
        return item;
    }

    /** 获取订单列表 */
    public PageResult<OrderVO> getOrderList(Long userId, String status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null && !"all".equals(status)) {
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

    /** 根据ID查询订单 */
    public Order getOrderById(Long orderId, Long userId) {
        return orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
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
        if (!OrderConstant.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_CANNOT_CANCEL);
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        for (OrderItem item : items) {
            Artwork artwork = artworkMapper.selectById(item.getArtworkId());
            artwork.setStock(artwork.getStock() + item.getQuantity());
            if (ProductConstant.STATUS_SOLD_OUT.equals(artwork.getStatus())) {
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
        if (!OrderConstant.STATUS_SHIPPED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_CANNOT_CONFIRM);
        }

        order.setStatus(OrderConstant.STATUS_COMPLETED);
        order.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 申请售后 */
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
        if (!OrderConstant.STATUS_COMPLETED.equals(order.getStatus()) && 
            !OrderConstant.STATUS_SHIPPED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不允许申请售后");
        }

        order.setStatus(OrderConstant.STATUS_REFUNDING);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 微信支付统一下单 */
    public String unifiedOrder(Long orderId, Long userId) {
        return unifiedOrder(orderId, userId, null);
    }

    /** 微信支付统一下单 (支持支付方式) */
    public String unifiedOrder(Long orderId, Long userId, String openId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!OrderConstant.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态不允许支付");
        }

        // 计算订单金额(转为分)
        long totalAmount = order.getPayAmount().multiply(BigDecimal.valueOf(100)).longValue();
        
        // 商品描述
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        String description = items.isEmpty() ? "艺术品购买" : items.get(0).getTitle();
        if (description.length() > 50) {
            description = description.substring(0, 47) + "...";
        }

        try {
            String codeUrl;
            
            if (openId != null && !openId.isEmpty()) {
                // JSAPI支付 (小程序/公众号)
                Map<String, String> jsApiResult = wxPayService.unifiedOrderJsApi(
                        order.getOrderNo(), 
                        String.valueOf(totalAmount), 
                        openId, 
                        description
                );
                codeUrl = jsApiResult.get("prepay_id");
            } else {
                // Native支付 (二维码支付)
                codeUrl = wxPayService.unifiedOrderNative(
                        order.getOrderNo(), 
                        String.valueOf(totalAmount), 
                        description
                );
            }
            
            // 存入Redis，设置支付过期时间（30分钟）
            redisTemplate.opsForValue().set("pay:order:" + orderId, order.getOrderNo(), 30, TimeUnit.MINUTES);
            
            log.info("微信支付下单成功 - 订单ID: {}, OrderNo: {}, codeUrl: {}", 
                    orderId, order.getOrderNo(), codeUrl);
            
            return codeUrl;
            
        } catch (Exception e) {
            log.error("微信支付统一下单失败", e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付下单失败: " + e.getMessage());
        }
    }

    /** 微信支付统一下单 - 返回完整支付参数 */
    public Map<String, Object> unifiedOrderWithParams(Long orderId, Long userId, String openId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!OrderConstant.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态不允许支付");
        }

        long totalAmount = order.getPayAmount().multiply(BigDecimal.valueOf(100)).longValue();
        
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        String description = items.isEmpty() ? "艺术品购买" : items.get(0).getTitle();

        try {
            Map<String, String> jsApiResult = wxPayService.unifiedOrderJsApi(
                    order.getOrderNo(), 
                    String.valueOf(totalAmount), 
                    openId, 
                    description
            );
            
            String prepayId = jsApiResult.get("prepay_id");
            
            redisTemplate.opsForValue().set("pay:order:" + orderId, order.getOrderNo(), 30, TimeUnit.MINUTES);
            
            Map<String, Object> payParams = new HashMap<>();
            payParams.put("prepay_id", prepayId);
            payParams.put("order_no", order.getOrderNo());
            payParams.put("pay_amount", order.getPayAmount());
            payParams.put("description", description);
            
            return payParams;
            
        } catch (Exception e) {
            log.error("微信支付统一下单失败", e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付下单失败: " + e.getMessage());
        }
    }

    /** 查询支付状态 */
    public Map<String, String> queryPayStatus(String orderNo) {
        try {
            Map<String, String> result = wxPayService.queryOrder(orderNo);
            
            Map<String, String> response = new HashMap<>();
            response.put("trade_state", result.get("trade_state"));
            response.put("trade_state_desc", result.get("trade_state_desc"));
            response.put("transaction_id", result.get("transaction_id"));
            response.put("total_fee", result.get("total_fee"));
            
            return response;
        } catch (Exception e) {
            log.error("查询支付状态失败", e);
            Map<String, String> response = new HashMap<>();
            response.put("trade_state", "ERROR");
            response.put("trade_state_desc", "查询失败");
            return response;
        }
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

        // 支付成功后计算并发放佣金（二级分销+团队奖励）
        try {
            calculateCommission(order);
        } catch (Exception e) {
            log.error("佣金计算失败", e);
            // 佣金计算失败不影响订单状态
        }
    }

    /**
     * 计算并发放佣金（二级分销+团队奖励）
     * 一级佣金：直接推广佣金（5%）
     * 二级佣金：团队奖励（2%）
     */
    private void calculateCommission(Order order) {
        // 获取订单关联的艺荐官ID
        Long promoterId = getPromoterIdByOrder(order.getId());
        if (promoterId == null) {
            log.info("订单无关联艺荐官，跳过佣金计算");
            return;
        }

        // 订单金额（分）
        long orderAmount = order.getPayAmount().multiply(new BigDecimal("100")).longValue();
        
        // ========== 一级佣金：直接推广佣金 ==========
        long directCommission = new BigDecimal(orderAmount)
                .multiply(DIRECT_COMMISSION_RATE).longValue();
        if (directCommission > 0) {
            log.info("一级佣金 - promoterId:{}, amount:{}", promoterId, directCommission);
            // 佣金记录和更新会在后续单独的服务中处理
            // 这里先更新订单的佣金金额
            order.setCommissionAmount(new BigDecimal(directCommission).divide(new BigDecimal("100")));
            orderMapper.updateById(order);
        }

        // ========== 二级佣金：团队奖励 ==========
        // 如果购买者也是艺荐官，给其上级发放团队奖励
        // 注意：团队奖励逻辑需要用户模块支持，这里预留扩展点
        log.info("订单佣金计算完成 - orderId:{}, directCommission:{}", order.getId(), directCommission);
    }

    /** 获取订单关联的艺荐官ID */
    private Long getPromoterIdByOrder(Long orderId) {
        // 从订单项获取艺荐官ID
        OrderItem orderItem = orderItemMapper.selectOne(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .isNotNull(OrderItem::getPromoterId)
        );
        return orderItem != null ? orderItem.getPromoterId() : null;
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
        if (address.getIsDefault() == null || address.getIsDefault().equals(1)) {
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
        if (addressUpdate.getReceiverPhone() != null) {
            address.setReceiverPhone(addressUpdate.getReceiverPhone());
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
            if (addressUpdate.getIsDefault().equals(1)) {
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

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0L);
        vo.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount().longValue() : 0L);
        vo.setPayAmount(order.getPayAmount() != null ? order.getPayAmount().longValue() : 0L);
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

        vo.setSourceText(getSourceText(order.getSource()));
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
            itemVO.setAuthorName("");
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getSubtotal());
            return itemVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    private String getSourceText(String source) {
        if (source == null) return "未知";
        return switch (source) {
            case OrderConstant.SOURCE_DIRECT -> "立即购买";
            case OrderConstant.SOURCE_CART -> "购物车";
            case OrderConstant.SOURCE_AUCTION -> "拍卖";
            default -> "未知";
        };
    }

    private String getStatusText(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case OrderConstant.STATUS_CANCELLED -> "已取消";
            case OrderConstant.STATUS_PENDING_PAYMENT -> "待付款";
            case OrderConstant.STATUS_PAID -> "已付款";
            case OrderConstant.STATUS_SHIPPED -> "已发货";
            case OrderConstant.STATUS_RECEIVED -> "已收货";
            case OrderConstant.STATUS_COMPLETED -> "已完成";
            case OrderConstant.STATUS_REFUNDING -> "退款中";
            case OrderConstant.STATUS_REFUNDED -> "已退款";
            default -> "未知";
        };
    }
}
