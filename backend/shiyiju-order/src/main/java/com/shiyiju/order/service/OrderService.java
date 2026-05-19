package com.shiyiju.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.constant.OrderConstant;
import com.shiyiju.common.constant.ProductConstant;
import com.shiyiju.common.client.WalletRestClient;
import com.shiyiju.common.event.FinanceEvent;
import com.shiyiju.common.event.FinanceEventPublisher;
import com.shiyiju.common.event.FinanceEventType;
import com.shiyiju.common.entity.Address;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.mapper.AddressMapper;
import com.shiyiju.common.order.OrderFailReason;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.common.service.WxPayService;
import com.shiyiju.order.dto.CreateOrderDTO;
import com.shiyiju.order.entity.*;
import com.shiyiju.order.exception.OrderFailCreateException;
import com.shiyiju.order.mapper.*;
import com.shiyiju.order.vo.AddressVO;
import com.shiyiju.order.vo.CartVO;
import com.shiyiju.order.vo.OrderItemVO;
import com.shiyiju.order.vo.OrderVO;
import com.shiyiju.common.entity.Artwork;
import com.shiyiju.common.entity.User;
import com.shiyiju.common.mapper.ArtworkMapper;
import com.shiyiju.common.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WxPayService wxPayService;
    private final WalletRestClient walletClient;
    private final FinanceEventPublisher financeEventPublisher;
    private final OrderFailRecorder orderFailRecorder;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    // 佣金比例
    private static final BigDecimal DIRECT_COMMISSION_RATE = new BigDecimal("0.05"); // 一级佣金 5%

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
            vo.setSubtotal(artwork.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            vo.setStock(artwork.getStock());
            vo.setSelected(false);
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** 添加到购物车 */
    @Transactional(rollbackFor = Exception.class)
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

    /** 从购物车创建订单（带异常捕获与失败记录） */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrderFromCart(Long userId, CreateOrderDTO dto) {
        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
            for (Long cartId : dto.getCartIds()) {
                String itemLockKey = "cart:item:lock:" + cartId;
                Object lockedUserId = redisTemplate.opsForValue().get(itemLockKey);
                if (lockedUserId == null || !userId.toString().equals(lockedUserId.toString())) {
                    throw new OrderFailCreateException(OrderFailReason.PARAM_INVALID,
                            "购物车项未锁定或已过期，请重新选择");
                }
            }
        }
        return executeOrderCreation(userId, dto, "CART");
    }

    /** 直接购买（带异常捕获与失败记录） */
    @Transactional(rollbackFor = Exception.class)
    public Order createDirectOrder(Long userId, CreateOrderDTO dto) {
        return executeOrderCreation(userId, dto, "DIRECT");
    }

    /**
     * 有保护的订单创建执行器
     * 统一管理事务边界、异常捕获、失败记录与自动回滚
     */
    public Order executeOrderCreation(Long userId, CreateOrderDTO dto, String source) {
        try {
            return createOrderInternal(userId, dto);
        } catch (OrderFailCreateException e) {
            // 已结构化的业务异常，直接记录失败原因
            log.warn("订单创建失败(业务) - userId={}, artworkId={}, source={}, reason={}, detail={}",
                    userId, dto.getArtworkId(), source, e.getFailReason().getCode(), e.getMessage());
            recordFailAsync(userId, dto, source, e.getFailReason(), e.getMessage(), null);
            throw e;
        } catch (BusinessException e) {
            // 现有业务异常，映射为订单失败原因
            OrderFailReason reason = mapBusinessExceptionToFailReason(e);
            log.warn("订单创建失败(业务异常) - userId={}, code={}, message={}",
                    userId, e.getCode(), e.getMessage());
            recordFailAsync(userId, dto, source, reason, e.getMessage(), null);
            throw new OrderFailCreateException(reason, e.getMessage());
        } catch (Exception e) {
            // 不可预料的系统异常，记录完整信息到日志
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("订单创建失败(系统异常) - userId={}, artworkId={}, source={}, exType={}, detail={}",
                    userId, dto.getArtworkId(), source, e.getClass().getName(), errMsg, e);
            recordFailAsync(userId, dto, source, OrderFailReason.INTERNAL_ERROR, errMsg, null);
            throw new OrderFailCreateException(OrderFailReason.INTERNAL_ERROR, "系统繁忙，请稍后重试");
        }
    }

    /**
     * 在独立事务中记录失败日志（不影响主事务回滚）
     */
    private void recordFailAsync(Long userId, CreateOrderDTO dto, String source,
                                 OrderFailReason reason, String message, String orderNo) {
        try {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = transactionManager.getTransaction(def);
            try {
                orderFailRecorder.record(OrderFailRecorder.RecordContext.builder()
                        .userId(userId)
                        .orderNo(orderNo)
                        .artworkId(dto.getArtworkId())
                        .cartIds(dto.getCartIds() != null ? dto.getCartIds().stream()
                                .map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) : null)
                        .source(source)
                        .failReason(reason)
                        .failMessage(message)
                        .requestParams(dto)
                        .compensated(orderNo != null)  // 已生成订单号的需要补偿
                );
                transactionManager.commit(status);
            } catch (Exception ex) {
                transactionManager.rollback(status);
                log.error("订单失败记录落库异常(已回滚)", ex);
            }
        } catch (Exception e) {
            log.error("订单失败记录事务启动失败", e);
        }
    }

    /**
     * 内部订单创建方法（事务由外层公共方法保障）
     * 外部调用请使用 createDirectOrder / createOrderFromCart
     */
    public Order createOrderInternal(Long userId, CreateOrderDTO dto) {
        // 获取地址：优先使用传入的地址；-1 或无匹配时使用默认地址
        Address address = resolveUserAddress(userId, dto.getAddressId());

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

                if (safeStock(artwork) < cart.getQuantity()) {
                    throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, "作品【" + artwork.getTitle() + "】库存不足");
                }

                OrderItem item = createOrderItem(artwork, cart.getQuantity());
                orderItems.add(item);
                totalAmount = totalAmount.add(safePrice(artwork).multiply(BigDecimal.valueOf(cart.getQuantity())));
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
            if (safeStock(artwork) < qty) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }

            OrderItem item = createOrderItem(artwork, qty);
            orderItems.add(item);
            totalAmount = totalAmount.add(safePrice(artwork).multiply(BigDecimal.valueOf(qty)));
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
        
        // 设置卖家信息（从第一个订单项的作者获取）
        if (!orderItems.isEmpty()) {
            OrderItem firstItem = orderItems.get(0);
            if (firstItem.getArtistId() != null) {
                User seller = userMapper.selectById(firstItem.getArtistId());
                if (seller != null) {
                    order.setSellerName(seller.getNickname());
                    order.setSellerAvatar(seller.getAvatar());
                }
            }
        }
        
        orderMapper.insert(order);

        // 保存订单项
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            item.setCreateTime(LocalDateTime.now());
            orderItemMapper.insert(item);

            // 扣减库存（空安全保护）
            Artwork artwork = artworkMapper.selectById(item.getArtworkId());
            if (artwork != null) {
                int currentStock = safeStock(artwork);
                int newStock = Math.max(currentStock - item.getQuantity(), 0);
                artwork.setStock(newStock);
                if (newStock <= 0) {
                    artwork.setStatus(ProductConstant.STATUS_SOLD_OUT);
                }
                artworkMapper.updateById(artwork);
            }
        }

        return order;
    }

    /**
     * 重试创建订单 — 用于 OrderFailController 手动重试
     * 根据失败记录中的参数重建订单
     */
    @Transactional
    public Order retryCreateOrder(OrderFailRecord record, int retryCount) {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setArtworkId(record.getArtworkId());
        dto.setQuantity(1);
        dto.setAddressId(-1L);  // 使用默认地址（或从失败记录中获取）

        // 从 requestParams JSON 恢复更多参数
        if (record.getRequestParams() != null) {
            try {
                CreateOrderDTO originalDto = objectMapper.readValue(record.getRequestParams(), CreateOrderDTO.class);
                if (originalDto != null) {
                    dto.setQuantity(originalDto.getQuantity() != null ? originalDto.getQuantity() : 1);
                    dto.setAddressId(originalDto.getAddressId() != null ? originalDto.getAddressId() : -1L);
                    dto.setRemark(originalDto.getRemark());
                    if (originalDto.getCartIds() != null) dto.setCartIds(originalDto.getCartIds());
                }
            } catch (Exception e) {
                log.warn("重试订单解析请求参数失败，使用默认参数: {}", e.getMessage());
            }
        }

        return createOrderInternal(record.getUserId(), dto);
    }

    /**
     * 将 BusinessException 映射为 OrderFailReason
     */
    private OrderFailReason mapBusinessExceptionToFailReason(BusinessException e) {
        if (e.getCode() == null) return OrderFailReason.INTERNAL_ERROR;
        switch (e.getCode()) {
            case 1201: return OrderFailReason.PRODUCT_OFF_SHELF;
            case 1202: return OrderFailReason.PRODUCT_OFF_SHELF;
            case 1203: return OrderFailReason.PRODUCT_SOLD_OUT;
            case 1204: return OrderFailReason.STOCK_INSUFFICIENT;
            case 1304: return OrderFailReason.PRICE_CHANGED;
            case 1402: return OrderFailReason.PAYMENT_TIMEOUT;
            default:
                // 检查消息内容，更准确地推断失败原因
                String msg = e.getMessage();
                if (msg != null) {
                    if (msg.contains("地址") || msg.contains("address")) return OrderFailReason.ADDRESS_INVALID;
                    if (msg.contains("用户") || msg.contains("user") || msg.contains("登录")) return OrderFailReason.USER_INVALID;
                    if (msg.contains("参数") || msg.contains("param") || msg.contains("stock")) return OrderFailReason.PARAM_INVALID;
                    if (msg.contains("库存") || msg.contains("stock")) return OrderFailReason.STOCK_INSUFFICIENT;
                    if (msg.contains("下架") || msg.contains("off shelf") || msg.contains("OFF_SHELF")) return OrderFailReason.PRODUCT_OFF_SHELF;
                    if (msg.contains("售罄") || msg.contains("sold out") || msg.contains("SOLD_OUT")) return OrderFailReason.PRODUCT_SOLD_OUT;
                }
                return OrderFailReason.INTERNAL_ERROR;
        }
    }

    /**
     * 解析用户地址：优先使用传入 addressId；-1 或无效时查询默认地址
     */
    private Address resolveUserAddress(Long userId, Long addressId) {
        if (addressId != null && addressId > 0) {
            Address address = addressMapper.selectById(addressId);
            if (address != null) return address;
        }
        // 查询用户默认地址
        List<Address> addresses = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getCreateTime)
                        .last("LIMIT 1")
        );
        if (!addresses.isEmpty()) {
            return addresses.get(0);
        }
        // 没有地址时允许无地址创建订单（数字艺术品场景），不阻塞购买
        Address fallback = new Address();
        fallback.setId(0L);
        fallback.setUserId(userId);
        fallback.setReceiverName("用户");
        fallback.setReceiverPhone("");
        fallback.setProvince("");
        fallback.setCity("");
        fallback.setDistrict("");
        fallback.setDetailAddress("平台托管");
        return fallback;
    }

    private OrderItem createOrderItem(Artwork artwork, int quantity) {
        BigDecimal price = safePrice(artwork);
        OrderItem item = new OrderItem();
        item.setArtworkId(artwork.getId());
        item.setArtistId(artwork.getAuthorId());  // 使用 authorId
        item.setItemType("ARTWORK");
        item.setTitle(artwork.getTitle());
        item.setCoverImage(artwork.getCoverImage());
        item.setPrice(price);
        item.setQuantity(quantity);
        item.setSubtotal(price.multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    /**
     * 空安全的库存获取
     */
    private int safeStock(Artwork artwork) {
        return artwork.getStock() != null ? artwork.getStock() : 999;
    }

    /**
     * 空安全的价格获取
     */
    private BigDecimal safePrice(Artwork artwork) {
        return artwork.getPrice() != null ? artwork.getPrice() : BigDecimal.ZERO;
    }

    /** 转售购买 - 创建转售订单 */
    @Transactional(rollbackFor = Exception.class)
    public Order createResaleOrder(Long userId, Long resaleId, BigDecimal resalePrice, Long artworkId, Long addressId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收货地址不存在");
        }

        // 查询作品信息
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // 创建订单项
        OrderItem item = new OrderItem();
        item.setArtworkId(artwork.getId());
        item.setArtistId(artwork.getAuthorId());
        item.setItemType("ARTWORK");
        item.setTitle(artwork.getTitle());
        item.setCoverImage(artwork.getCoverImage());
        item.setPrice(resalePrice);
        item.setQuantity(1);
        item.setSubtotal(resalePrice);
        List<OrderItem> orderItems = Collections.singletonList(item);

        // 生成订单号
        String orderNo = "SYJ" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%04d", userId % 10000);

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(resalePrice);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(resalePrice);
        order.setCommissionAmount(BigDecimal.ZERO);
        order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        order.setRemark("resale:" + resaleId);  // 记录转售ID用于支付回调
        order.setSource(OrderConstant.SOURCE_RESALE);
        order.setStatus(OrderConstant.STATUS_PENDING_PAYMENT);
        order.setCreateTime(LocalDateTime.now());

        // 设置卖家信息
        if (artwork.getAuthorId() != null) {
            User seller = userMapper.selectById(artwork.getAuthorId());
            if (seller != null) {
                order.setSellerName(seller.getNickname());
                order.setSellerAvatar(seller.getAvatar());
            }
        }

        orderMapper.insert(order);

        // 保存订单项
        item.setOrderId(order.getId());
        item.setCreateTime(LocalDateTime.now());
        orderItemMapper.insert(item);

        log.info("创建转售订单: orderId={}, orderNo={}, resaleId={}, userId={}, amount={}",
                order.getId(), orderNo, resaleId, userId, resalePrice);
        return order;
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
        order.setPaymentStatus(OrderConstant.STATUS_REFUNDING);
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

        // 订单金额沿用商品链路的“分”单位，微信支付也要求传分，不能再次放大 100 倍
        long totalAmount = order.getPayAmount().longValue();
        
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

        long totalAmount = order.getPayAmount().longValue();
        
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
            
            redisTemplate.opsForValue().set("pay:order:" + orderId, order.getOrderNo(), 30, TimeUnit.MINUTES);
            
            // 直接返回微信支付所需的完整参数（含 appId, timeStamp, nonceStr, package, signType, paySign）
            Map<String, Object> payParams = new HashMap<>(jsApiResult);
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
    @Transactional(rollbackFor = Exception.class)
    public void handlePayCallback(String orderNo, String transactionId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        if (order == null) return;

        // 幂等
        if (!OrderConstant.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            log.info("订单 {} 已处理，幂等返回，当前状态: {}", orderNo, order.getStatus());
            return;
        }

        order.setStatus(OrderConstant.STATUS_PAID);
        order.setPaymentStatus(OrderConstant.STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // === 转售订单：发布事件标记已支付（异步处理，不阻塞本地事务） ===
        if (OrderConstant.SOURCE_RESALE.equals(order.getSource())) {
            Long resaleId = parseResaleIdFromRemark(order.getRemark());
            if (resaleId != null) {
                financeEventPublisher.publish(FinanceEvent.builder()
                        .type(FinanceEventType.RESALE_MARK_PAID)
                        .resaleId(resaleId)
                        .buyerUserId(order.getUserId())
                        .orderNo(orderNo)
                        .build());
                log.info("转售订单事件已发布: orderId={}, resaleId={}, buyerId={}",
                        order.getId(), resaleId, order.getUserId());
            }
            return;
        }

        // === 普通订单：为每个订单项发布艺术家收益事件 ===
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            Artwork artwork = artworkMapper.selectById(item.getArtworkId());
            if (artwork != null && artwork.getAuthorId() != null && item.getPrice() != null
                    && item.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                financeEventPublisher.publish(FinanceEvent.builder()
                        .type(FinanceEventType.ARTIST_INCOME)
                        .userId(artwork.getAuthorId())
                        .amount(item.getPrice())
                        .relatedId(order.getId())
                        .relatedType("order")
                        .remark("作品销售: " + artwork.getTitle() + " " + orderNo)
                        .build());
            }
        }

        // === 普通订单：发布佣金结算事件 ===
        Long promoterId = getPromoterIdByOrder(order.getId());
        if (promoterId != null) {
            Long artworkId = getFirstArtworkId(order.getId());
            financeEventPublisher.publish(FinanceEvent.builder()
                    .type(FinanceEventType.COMMISSION_SETTLE)
                    .userId(promoterId)
                    .amount(order.getPayAmount())
                    .relatedId(order.getId())
                    .relatedType("order")
                    .orderNo(orderNo)
                    .remark("推广佣金 " + orderNo)
                    .artworkId(artworkId)
                    .build());
        }
    }

    /**
     * 从订单备注中解析转售ID（格式: "resale:123"）
     */
    private Long parseResaleIdFromRemark(String remark) {
        if (remark == null || remark.isEmpty()) return null;
        if (remark.startsWith("resale:")) {
            try {
                return Long.parseLong(remark.substring("resale:".length()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 结算艺术家收益 - 通过发布事件异步处理
     */
    private void settleArtistIncome(Order order) {
        // 已迁移到 handlePayCallback 中的事件发布逻辑
    }

    /**
     * 计算并发放佣金（二级分销+团队奖励）
     * 已迁移到 handlePayCallback 中的事件发布逻辑
     */
    private void calculateCommission(Order order) {
        // 已迁移到 handlePayCallback 中的事件发布逻辑
    }

    /** 获取订单第一个作品ID */
    public Long getFirstArtworkId(Long orderId) {
        OrderItem item = orderItemMapper.selectOne(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .last("LIMIT 1"));
        return item != null ? item.getArtworkId() : null;
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
        vo.setTotalAmount(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
        vo.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        vo.setPayAmount(order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO);
        
        // 构建地址VO
        AddressVO addressVO = new AddressVO();
        addressVO.setReceiverName(order.getReceiverName());
        addressVO.setReceiverPhone(order.getReceiverPhone());
        addressVO.setFullAddress(order.getReceiverAddress());
        // 尝试解析地址（格式：省-市-区-详情）
        if (order.getReceiverAddress() != null) {
            String[] parts = order.getReceiverAddress().split("-");
            if (parts.length >= 4) {
                addressVO.setProvince(parts[0]);
                addressVO.setCity(parts[1]);
                addressVO.setDistrict(parts[2]);
                addressVO.setDetail(parts[3]);
            } else if (parts.length == 3) {
                addressVO.setProvince(parts[0]);
                addressVO.setCity(parts[1]);
                addressVO.setDistrict(parts[2]);
                addressVO.setDetail("");
            } else {
                addressVO.setProvince("");
                addressVO.setCity("");
                addressVO.setDistrict("");
                addressVO.setDetail(order.getReceiverAddress());
            }
        }
        vo.setAddress(addressVO);
        
        vo.setRemark(order.getRemark());
        vo.setSource(order.getSource());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime() != null ? order.getPayTime().toString() : null);
        vo.setShipTime(order.getShipTime() != null ? order.getShipTime().toString() : null);
        vo.setReceiveTime(order.getReceiveTime() != null ? order.getReceiveTime().toString() : null);
        vo.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : null);
        
        // 运费
        vo.setFreight(order.getFreightAmount() != null ? order.getFreightAmount() : BigDecimal.ZERO);
        // 物流信息
        vo.setTrackingNo(order.getTrackingNo());
        vo.setExpressName(order.getExpressName());

        vo.setSourceText(getSourceText(order.getSource()));
        vo.setStatusText(getStatusText(order.getStatus()));

        // 卖家信息（从第一个订单项的作者获取）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        
        // 如果订单没有卖家信息，尝试从订单项获取
        if ((order.getSellerName() == null || order.getSellerName().isEmpty()) && !items.isEmpty()) {
            OrderItem firstItem = items.get(0);
            if (firstItem.getArtistId() != null) {
                User seller = userMapper.selectById(firstItem.getArtistId());
                if (seller != null) {
                    order.setSellerName(seller.getNickname());
                    order.setSellerAvatar(seller.getAvatar());
                }
            }
        }
        
        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setArtworkId(item.getArtworkId());
            itemVO.setTitle(item.getTitle());
            itemVO.setCoverImage(item.getCoverImage());
            itemVO.setAuthorName(item.getAuthorName());
            itemVO.setArtistName(item.getAuthorName());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getSubtotal());
            return itemVO;
        }).collect(Collectors.toList()));

        // 设置卖家信息到VO
        vo.setSellerName(order.getSellerName());
        vo.setSellerAvatar(order.getSellerAvatar());

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
