package com.shiyiju.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.constant.OrderConstant;
import com.shiyiju.common.constant.ProductConstant;
import com.shiyiju.common.client.CommissionRestClient;
import com.shiyiju.common.client.ResaleRestClient;
import com.shiyiju.common.client.WalletRestClient;
import com.shiyiju.common.config.WxPayConfig;
import com.shiyiju.common.event.FinanceEvent;
import com.shiyiju.common.event.FinanceEventPublisher;
import com.shiyiju.common.event.FinanceEventType;
import com.shiyiju.common.entity.Address;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.mapper.AddressMapper;
import com.shiyiju.common.order.OrderFailReason;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.common.service.AlipayService;
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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private final WxPayConfig wxPayConfig;
    private final WxPayService wxPayService;
    private final AlipayService alipayService;
    private final ResaleRestClient resaleRestClient;
    private final WalletRestClient walletClient;
    private final CommissionRestClient commissionRestClient;
    private final FinanceEventPublisher financeEventPublisher;
    private final PaymentService paymentService;
    private final OrderFailRecorder orderFailRecorder;
    private final LogisticsMapper logisticsMapper;
    private final LogisticsService logisticsService;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate productRestTemplate = new RestTemplate();

    @Value("${resale.platform-wallet-user-id:1}")
    private Long platformWalletUserId;

    @Value("${shiyiju.services.product-url:http://shiyiju-product:8082}")
    private String productServiceUrl;

    @Value("${order.payment-expire-minutes:30}")
    private int paymentExpireMinutes;

    @Value("${order.payment-expiry-grace-minutes:5}")
    private int paymentExpiryGraceMinutes;

    @PostConstruct
    public void initArtworkFreightColumn() {
        try {
            addColumnIfMissing("artwork", "freight", "DECIMAL(10,2) DEFAULT 0 COMMENT '运费（元）'");
            addColumnIfMissing("trade_order", "seller_user_id", "BIGINT DEFAULT NULL COMMENT '卖家用户ID'");
            addColumnIfMissing("trade_order", "request_id", "VARCHAR(64) DEFAULT NULL COMMENT '下单幂等号'");
            addColumnIfMissing("trade_order", "pay_expire_time", "DATETIME DEFAULT NULL COMMENT '待付款失效时间'");
            addColumnIfMissing("trade_order", "cancel_reason", "VARCHAR(255) DEFAULT NULL COMMENT '取消原因'");
            ensureStockReservationTable();
            ensureIndex("trade_order", "uk_order_buyer_request",
                    "CREATE UNIQUE INDEX uk_order_buyer_request ON trade_order (buyer_user_id, request_id)");
            backfillHistoricalSellerUserIds();
        } catch (Exception e) {
            log.warn("初始化订单卖家字段失败，后续下单时将重试", e);
        }
    }

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String RESERVATION_RESERVED = "RESERVED";
    private static final String RESERVATION_CONFIRMED = "CONFIRMED";
    private static final String RESERVATION_RELEASED = "RELEASED";
    
    // 佣金比例
    private static final BigDecimal DIRECT_COMMISSION_RATE = new BigDecimal("0.05"); // 一级佣金 5%
    private static final String PLATFORM_COMMISSION_ENABLED_KEY = "platform.commission.enabled";
    private static final String PLATFORM_COMMISSION_PRIMARY_RATE_KEY = "platform.commission.primary.sale.rate";
    private static final String PLATFORM_COMMISSION_MIN_FEE_KEY = "platform.commission.min.fee";
    private static final String PLATFORM_COMMISSION_WALLET_UID_KEY = "platform.commission.wallet.uid";

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private void ensureIndex(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureStockReservationTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS order_stock_reservation (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    artwork_id BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_order_artwork (order_id, artwork_id),
                    KEY idx_reservation_status (status),
                    KEY idx_reservation_artwork (artwork_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单库存预占流水'
                """);
    }

    private void ensureSellerUserIdReady() {
        addColumnIfMissing("trade_order", "seller_user_id", "BIGINT DEFAULT NULL COMMENT '卖家用户ID'");
        backfillHistoricalSellerUserIds();
    }

    private void backfillHistoricalSellerUserIds() {
        if (!columnExists("trade_order", "seller_user_id")) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, order_type, remark
                FROM trade_order
                WHERE deleted = 0
                  AND seller_user_id IS NULL
                ORDER BY id DESC
                LIMIT 500
                """);
        for (Map<String, Object> row : rows) {
            Long orderId = toLong(row.get("id"));
            if (orderId == null) {
                continue;
            }
            Long sellerUserId = resolveHistoricalSellerUserId(
                    orderId,
                    stringValue(row.get("order_type")),
                    stringValue(row.get("remark"))
            );
            if (sellerUserId == null || sellerUserId <= 0) {
                continue;
            }
            try {
                jdbcTemplate.update("UPDATE trade_order SET seller_user_id = ? WHERE id = ? AND seller_user_id IS NULL",
                        sellerUserId, orderId);
            } catch (Exception e) {
                log.warn("回填订单卖家ID失败: orderId={}, sellerUserId={}", orderId, sellerUserId, e);
            }
        }
    }

    private Long resolveHistoricalSellerUserId(Long orderId, String orderSource, String remark) {
        if (OrderConstant.SOURCE_RESALE.equals(orderSource)) {
            Long resaleId = parseResaleId(remark);
            if (resaleId != null) {
                Map<String, Object> resale = resaleRestClient.getDetail(resaleId);
                Long resaleSellerUserId = toLong(resale != null ? resale.get("sellerUserId") : null);
                if (resaleSellerUserId != null && resaleSellerUserId > 0) {
                    return resaleSellerUserId;
                }
            }
        }
        OrderItem firstItem = orderItemMapper.selectOne(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .orderByAsc(OrderItem::getId)
                        .last("LIMIT 1")
        );
        return firstItem != null ? firstItem.getArtistId() : null;
    }

    private Long parseResaleId(String remark) {
        if (remark == null || !remark.startsWith("resale:")) {
            return null;
        }
        return toLong(remark.substring("resale:".length()));
    }

    private void ensureCartTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_cart (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                artwork_id BIGINT NOT NULL,
                quantity INT NOT NULL DEFAULT 1,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                deleted TINYINT NOT NULL DEFAULT 0,
                UNIQUE KEY uk_user_cart_artwork_active (user_id, artwork_id, deleted),
                KEY idx_user_cart_user_id (user_id),
                KEY idx_user_cart_artwork_id (artwork_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户购物车'
            """);
    }

    /** 获取购物车列表 */
    public List<CartVO> getCartList(Long userId) {
        ensureCartTable();
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
            BigDecimal price = safePrice(artwork);
            vo.setPrice(price);
            vo.setQuantity(cart.getQuantity());
            vo.setSubtotal(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
            vo.setStock(artwork.getStock());
            vo.setSelected(false);
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** 添加到购物车 */
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(Long userId, Long artworkId, Integer quantity) {
        ensureCartTable();
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (artwork.getStatus() != ProductConstant.STATUS_ON_SALE) {
            throw new BusinessException(ResultCode.PRODUCT_OFF_SHELF);
        }
        if (!hasEnoughStockForOrder(artwork, quantity)) {
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
        ensureCartTable();
        cartMapper.delete(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .in(Cart::getId, cartIds)
        );
    }

    /** 更新购物车数量 */
    @Transactional
    public void updateCartQuantity(Long userId, Long cartId, Integer quantity) {
        ensureCartTable();
        Cart cart = cartMapper.selectOne(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getId, cartId)
                        .eq(Cart::getUserId, userId)
        );
        if (cart == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "购物车记录不存在");
        }
        
        Artwork artwork = artworkMapper.selectById(cart.getArtworkId());
        if (artwork != null && !hasEnoughStockForOrder(artwork, quantity)) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
        
        cart.setQuantity(quantity);
        cart.setUpdatedAt(LocalDateTime.now());
        cartMapper.updateById(cart);
    }

    /** 锁定购物车项（结算前）- 使用 Redis 防止超卖 */
    public Map<String, Object> lockCartItems(Long userId, List<Long> cartIds) {
        ensureCartTable();
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
                item.put("price", safePrice(artwork));
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
        ensureCartTable();
        for (Long cartId : cartIds) {
            String itemLockKey = "cart:item:lock:" + cartId;
            Object lockedUserId = redisTemplate.opsForValue().get(itemLockKey);
            if (lockedUserId != null && userId.toString().equals(lockedUserId.toString())) {
                redisTemplate.delete(itemLockKey);
            }
        }
    }

    /** 从购物车创建订单（带异常捕获与失败记录） */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrderFromCart(Long userId, CreateOrderDTO dto) {
        Order existingOrder = findExistingOrder(userId, dto.getRequestId());
        if (existingOrder != null) {
            return existingOrder;
        }
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
        Order existingOrder = findExistingOrder(userId, dto.getRequestId());
        if (existingOrder != null) {
            return existingOrder;
        }
        return executeOrderCreation(userId, dto, "DIRECT");
    }

    private Order findExistingOrder(Long userId, String requestId) {
        String normalizedRequestId = normalizeRequestId(requestId);
        if (normalizedRequestId == null) {
            return null;
        }
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRequestId, normalizedRequestId)
                .last("LIMIT 1"));
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        String normalized = requestId.trim();
        if (normalized.length() > 64) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "下单请求号长度不能超过64位");
        }
        return normalized;
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
        BigDecimal freightAmount = BigDecimal.ZERO;

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

                if (!hasEnoughStockForOrder(artwork, cart.getQuantity())) {
                    throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, "作品【" + artwork.getTitle() + "】库存不足");
                }

                OrderItem item = createOrderItem(artwork, cart.getQuantity(), null);
                orderItems.add(item);
                totalAmount = totalAmount.add(safePrice(artwork).multiply(BigDecimal.valueOf(cart.getQuantity())));
                freightAmount = freightAmount.add(safeFreight(artwork).multiply(BigDecimal.valueOf(cart.getQuantity())));
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
            if (!hasEnoughStockForOrder(artwork, qty)) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }

            OrderItem item = createOrderItem(artwork, qty, dto.getPromoterId());
            orderItems.add(item);
            totalAmount = totalAmount.add(safePrice(artwork).multiply(BigDecimal.valueOf(qty)));
            freightAmount = freightAmount.add(safeFreight(artwork).multiply(BigDecimal.valueOf(qty)));
        }

        if (orderItems.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单项为空");
        }

        // 生成订单号
        String orderNo = generateOrderNo();

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setRequestId(normalizeRequestId(dto.getRequestId()));
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFreightAmount(freightAmount);
        order.setPayAmount(totalAmount.add(freightAmount));
        order.setCommissionAmount(BigDecimal.ZERO);
        order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        order.setRemark(dto.getRemark());
        order.setSource(dto.getCartIds() != null ? OrderConstant.SOURCE_CART : OrderConstant.SOURCE_DIRECT);
        order.setStatus(OrderConstant.STATUS_PENDING_PAYMENT);
        order.setPaymentStatus("UNPAID");
        LocalDateTime createdAt = LocalDateTime.now();
        order.setCreateTime(createdAt);
        order.setPayExpireTime(createdAt.plusMinutes(Math.max(paymentExpireMinutes, 1)));
        
        // 设置卖家信息（从第一个订单项的作者获取）
        if (!orderItems.isEmpty()) {
            OrderItem firstItem = orderItems.get(0);
            if (firstItem.getArtistId() != null) {
                order.setSellerUserId(firstItem.getArtistId());
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
        }

        reserveOrderStock(order, orderItems);

        finalizeZeroAmountOrder(order);

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
                    dto.setRequestId(originalDto.getRequestId());
                    if (originalDto.getCartIds() != null) dto.setCartIds(originalDto.getCartIds());
                }
            } catch (Exception e) {
                log.warn("重试订单解析请求参数失败，使用默认参数: {}", e.getMessage());
            }
        }

        return createOrderInternal(record.getUserId(), dto);
    }

    private String generateOrderNo() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return "SYJ" + LocalDateTime.now().format(ORDER_NO_FORMAT) + random;
    }

    /**
     * 创建订单时原子预占库存。订单和预占流水处于同一数据库事务，任一商品失败都会整体回滚。
     */
    private void reserveOrderStock(Order order, List<OrderItem> items) {
        for (OrderItem item : items) {
            int quantity = Math.max(item.getQuantity() == null ? 1 : item.getQuantity(), 1);
            int updated = jdbcTemplate.update("""
                    UPDATE artwork
                    SET status = CASE
                            WHEN stock IS NULL OR stock <= 0 OR stock - ? <= 0 THEN ?
                            ELSE status
                        END,
                        stock = CASE
                            WHEN stock IS NULL OR stock <= 0 THEN 0
                            ELSE stock - ?
                        END,
                        update_time = CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND deleted = 0
                      AND status = ?
                      AND (((stock IS NULL OR stock <= 0) AND ? = 1) OR stock >= ?)
                    """,
                    quantity, ProductConstant.STATUS_SOLD_OUT, quantity,
                    item.getArtworkId(), ProductConstant.STATUS_ON_SALE, quantity, quantity);
            if (updated != 1) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH,
                        "作品【" + item.getTitle() + "】已售出或库存不足");
            }
            jdbcTemplate.update("""
                    INSERT INTO order_stock_reservation (order_id, artwork_id, quantity, status)
                    VALUES (?, ?, ?, ?)
                    """, order.getId(), item.getArtworkId(), quantity, RESERVATION_RESERVED);
        }
    }

    /** 取消或超时订单释放库存；通过流水状态条件更新保证重复调用不会重复回补。 */
    private void releaseOrderStock(Long orderId) {
        List<Map<String, Object>> reservations = jdbcTemplate.queryForList("""
                SELECT artwork_id, quantity
                FROM order_stock_reservation
                WHERE order_id = ? AND status = ?
                """, orderId, RESERVATION_RESERVED);
        for (Map<String, Object> reservation : reservations) {
            Long artworkId = toLong(reservation.get("artwork_id"));
            int quantity = Optional.ofNullable(toLong(reservation.get("quantity"))).orElse(1L).intValue();
            int released = jdbcTemplate.update("""
                    UPDATE order_stock_reservation
                    SET status = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE order_id = ? AND artwork_id = ? AND status = ?
                    """, RESERVATION_RELEASED, orderId, artworkId, RESERVATION_RESERVED);
            if (released == 1) {
                jdbcTemplate.update("""
                        UPDATE artwork
                        SET stock = COALESCE(stock, 0) + ?,
                            status = CASE WHEN status = ? THEN ? ELSE status END,
                            update_time = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = 0
                        """, quantity, ProductConstant.STATUS_SOLD_OUT,
                        ProductConstant.STATUS_ON_SALE, artworkId);
            }
        }
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

    private OrderItem createOrderItem(Artwork artwork, int quantity, Long promoterId) {
        BigDecimal price = safePrice(artwork);
        OrderItem item = new OrderItem();
        item.setArtworkId(artwork.getId());
        item.setArtistId(resolveArtworkSellerUserId(artwork));
        item.setItemType("ARTWORK");
        item.setTitle(artwork.getTitle());
        item.setCoverImage(artwork.getCoverImage());
        item.setPrice(price);
        item.setQuantity(quantity);
        item.setSubtotal(price.multiply(BigDecimal.valueOf(quantity)));
        item.setPromoterId(promoterId);
        return item;
    }

    private Long resolveArtworkSellerUserId(Artwork artwork) {
        if (artwork == null) {
            return null;
        }
        if (artwork.getHolderId() != null && artwork.getHolderId() > 0) {
            return artwork.getHolderId();
        }
        return artwork.getAuthorId();
    }

    /**
     * 作品是唯一标的时，历史数据里 stock 可能为 0/null，但只要仍处于上架状态就可购买 1 件。
     */
    private boolean hasEnoughStockForOrder(Artwork artwork, Integer quantity) {
        if (artwork == null) {
            return false;
        }
        int qty = Math.max(quantity == null ? 1 : quantity, 1);
        Integer stock = artwork.getStock();
        if (stock == null || stock <= 0) {
            return ProductConstant.STATUS_ON_SALE.equals(artwork.getStatus()) && qty == 1;
        }
        return stock >= qty;
    }

    /**
     * 下单价使用作品实时收藏价，和详情页展示口径保持一致。
     */
    private BigDecimal safePrice(Artwork artwork) {
        if (artwork == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal productCurrentPrice = resolveProductCurrentPrice(artwork);
        if (productCurrentPrice != null && productCurrentPrice.compareTo(BigDecimal.ZERO) > 0) {
            return productCurrentPrice;
        }
        BigDecimal basePrice = getArtworkBasePrice(artwork);
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal priceRise = artwork.getPriceRise();
        if (priceRise == null) {
            priceRise = calculateRuntimePriceRise(artwork);
        }
        return basePrice.multiply(BigDecimal.ONE.add(priceRise))
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal safeFreight(Artwork artwork) {
        if (artwork == null || artwork.getFreight() == null || artwork.getFreight().compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return artwork.getFreight().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveProductCurrentPrice(Artwork artwork) {
        if (artwork == null || artwork.getId() == null) {
            return null;
        }
        try {
            Map<?, ?> response = productRestTemplate.getForObject(
                    productServiceUrl.replaceAll("/+$", "") + "/product/" + artwork.getId(),
                    Map.class
            );
            if (response == null || response.get("data") == null) {
                return null;
            }
            Object data = response.get("data");
            if (!(data instanceof Map<?, ?> product)) {
                return null;
            }
            Object currentPrice = product.get("currentPrice");
            if (currentPrice == null) {
                return null;
            }
            return normalizeArtworkAmountScale(
                    new BigDecimal(currentPrice.toString()).setScale(0, RoundingMode.HALF_UP),
                    getArtworkBasePrice(artwork),
                    1,
                    true
            );
        } catch (Exception e) {
            log.warn("获取商品实时价格失败，使用订单服务本地价格兜底: artworkId={}, error={}",
                    artwork.getId(), e.getMessage());
            return null;
        }
    }

    private BigDecimal getArtworkBasePrice(Artwork artwork) {
        if (artwork == null) {
            return null;
        }
        if (artwork.getOriginalPrice() != null && artwork.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            return artwork.getOriginalPrice();
        }
        return artwork.getPrice();
    }

    /**
     * 历史数据里有一部分金额被重复放大 100 倍。
     * 对普通作品订单，涨幅理论上被限制在一个较低区间内，因此这里允许用作品基础价做尺度纠偏。
     */
    private BigDecimal normalizeArtworkAmountScale(BigDecimal amount, BigDecimal basePrice, int quantity, boolean allowGrowth) {
        if (amount == null) {
            return null;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return amount;
        }
        BigDecimal normalized = amount;
        BigDecimal reference = basePrice.multiply(BigDecimal.valueOf(Math.max(quantity, 1)));
        BigDecimal maxReasonable = allowGrowth
                ? reference.multiply(BigDecimal.TEN)
                : reference.multiply(new BigDecimal("1.5"));
        while (normalized.compareTo(maxReasonable) > 0) {
            normalized = normalized.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
        return normalized;
    }

    private BigDecimal calculateRuntimePriceRise(Artwork artwork) {
        BigDecimal totalMultiplier = BigDecimal.ONE;
        long days = 0;
        if (artwork.getCreateTime() != null) {
            days = ChronoUnit.DAYS.between(artwork.getCreateTime(), LocalDateTime.now());
            if (days < 0) days = 0;
        }
        long baseDays = Math.min(days, 30);
        long matureDays = Math.max(days - 30, 0);
        BigDecimal timeMultiplier = BigDecimal.ONE
                .add(new BigDecimal("0.0002").multiply(BigDecimal.valueOf(baseDays)))
                .add(new BigDecimal("0.0003").multiply(BigDecimal.valueOf(matureDays)));
        totalMultiplier = totalMultiplier.multiply(timeMultiplier);

        int displayViewCount = safeCount(artwork.getViewCount()) + safeCount(artwork.getDailyViewCount()) * Math.toIntExact(Math.min(days, Integer.MAX_VALUE));
        if (displayViewCount >= 100) {
            totalMultiplier = totalMultiplier.multiply(new BigDecimal("1.1"));
        }

        int displayLikeCount = safeCount(artwork.getFavoriteCount()) + safeCount(artwork.getDailyLikeCount()) * Math.toIntExact(Math.min(days, Integer.MAX_VALUE));
        if (displayLikeCount >= 5) {
            totalMultiplier = totalMultiplier.multiply(new BigDecimal("1.1"));
        }

        int sales = Math.min(safeCount(artwork.getSaleCount()), 10);
        for (int i = 0; i < sales; i++) {
            totalMultiplier = totalMultiplier.multiply(new BigDecimal("1.05"));
        }

        BigDecimal maxGrowthMultiple = new BigDecimal("5.0");
        if (totalMultiplier.compareTo(maxGrowthMultiple) > 0) {
            totalMultiplier = maxGrowthMultiple;
        }
        return totalMultiplier.subtract(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP);
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    /** 转售购买 - 创建转售订单 */
    @Transactional(rollbackFor = Exception.class)
    public Order createResaleOrder(Long userId, Long resaleId, BigDecimal resalePrice, Long artworkId, Long addressId) {
        Map<String, Object> resale = resaleRestClient.getDetail(resaleId);
        if (resale == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转售记录不存在");
        }
        String resaleStatus = String.valueOf(resale.getOrDefault("status", ""));
        if (!"pending".equals(resaleStatus)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该作品转售已失效");
        }
        Long sellerUserId = toLong(resale.get("sellerUserId"));
        if (sellerUserId != null && sellerUserId.equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能购买自己发布的转售作品");
        }
        Long resaleArtworkId = toLong(resale.get("artworkId"));
        if (resaleArtworkId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转售作品信息缺失");
        }
        if (artworkId != null && !resaleArtworkId.equals(artworkId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转售作品不匹配");
        }
        BigDecimal confirmedResalePrice = toBigDecimal(resale.get("resalePrice"));
        if (confirmedResalePrice == null || confirmedResalePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转售价格异常");
        }
        Address address = resolveUserAddress(userId, addressId);

        // 查询作品信息
        Artwork artwork = artworkMapper.selectById(resaleArtworkId);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // 创建订单项
        OrderItem item = new OrderItem();
        item.setArtworkId(artwork.getId());
        item.setArtistId(sellerUserId != null ? sellerUserId : resolveArtworkSellerUserId(artwork));
        item.setItemType("ARTWORK");
        item.setTitle(artwork.getTitle());
        item.setCoverImage(artwork.getCoverImage());
        item.setPrice(confirmedResalePrice);
        item.setQuantity(1);
        item.setSubtotal(confirmedResalePrice);
        List<OrderItem> orderItems = Collections.singletonList(item);

        // 生成订单号
        String orderNo = "SYJ" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%04d", userId % 10000);

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(confirmedResalePrice);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(confirmedResalePrice);
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
        if (sellerUserId != null) {
            order.setSellerUserId(sellerUserId);
            User seller = userMapper.selectById(sellerUserId);
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

        finalizeZeroAmountOrder(order);

        log.info("创建转售订单: orderId={}, orderNo={}, resaleId={}, userId={}, amount={}",
                order.getId(), orderNo, resaleId, userId, confirmedResalePrice);
        return order;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取订单列表 */
    public PageResult<OrderVO> getOrderList(Long userId, String status, Integer page, Integer pageSize) {
        normalizeZeroAmountPendingOrders(userId);
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

    /** 获取艺术家/卖家视角的已卖出订单 */
    public PageResult<OrderVO> getSellerOrderList(Long sellerUserId, String status, Integer page, Integer pageSize) {
        ensureSellerUserIdReady();

        LambdaQueryWrapper<Order> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Order::getSellerUserId, sellerUserId);
        if (status != null && !"all".equals(status)) {
            countWrapper.eq(Order::getStatus, status);
        }
        Long total = orderMapper.selectCount(countWrapper);
        if (total == null || total <= 0) {
            return PageResult.of(0L, page, pageSize, Collections.emptyList());
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getSellerUserId, sellerUserId);
        if (status != null && !"all".equals(status)) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> result = orderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<OrderVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(total, page, pageSize, voList);
    }

    /** 获取订单详情 */
    public OrderVO getOrderDetail(Long orderId, Long userId) {
        normalizeZeroAmountPendingOrders(userId);
        ensureSellerUserIdReady();
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .and(wrapper -> wrapper.eq(Order::getUserId, userId).or().eq(Order::getSellerUserId, userId))
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return convertToVO(order);
    }

    /** 根据ID查询订单 */
    public Order getOrderById(Long orderId, Long userId) {
        normalizeZeroAmountPendingOrders(userId);
        ensureSellerUserIdReady();
        return orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .and(wrapper -> wrapper.eq(Order::getUserId, userId).or().eq(Order::getSellerUserId, userId))
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

        LocalDateTime now = LocalDateTime.now();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, OrderConstant.STATUS_PENDING_PAYMENT)
                .set(Order::getStatus, OrderConstant.STATUS_CANCELLED)
                .set(Order::getCancelTime, now)
                .set(Order::getCancelReason, "USER_CANCELLED")
                .set(Order::getUpdateTime, now));
        if (updated != 1) {
            throw new BusinessException(ResultCode.ORDER_CANNOT_CANCEL);
        }
        releaseOrderStock(orderId);
    }

    /** 定时关闭已超过支付期限的订单，单批限制数量避免长事务。 */
    @Transactional(rollbackFor = Exception.class)
    public int expirePendingOrders(int batchSize) {
        int safeBatchSize = Math.min(Math.max(batchSize, 1), 500);
        List<Order> expiredOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderConstant.STATUS_PENDING_PAYMENT)
                .le(Order::getPayExpireTime,
                        LocalDateTime.now().minusMinutes(Math.max(paymentExpiryGraceMinutes, 0)))
                .orderByAsc(Order::getPayExpireTime)
                .last("LIMIT " + safeBatchSize));
        int expiredCount = 0;
        for (Order order : expiredOrders) {
            LocalDateTime now = LocalDateTime.now();
            int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, order.getId())
                    .eq(Order::getStatus, OrderConstant.STATUS_PENDING_PAYMENT)
                    .set(Order::getStatus, OrderConstant.STATUS_CANCELLED)
                    .set(Order::getCancelTime, now)
                    .set(Order::getCancelReason, "PAYMENT_TIMEOUT")
                    .set(Order::getUpdateTime, now));
            if (updated == 1) {
                releaseOrderStock(order.getId());
                expiredCount++;
            }
        }
        return expiredCount;
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
        logisticsService.confirmReceive(orderId, userId);
        releaseFrozenOrderSale(order);
    }

    /** 申请售后 */
    @Transactional
    public void applyRefund(Long orderId, Long userId, Map<String, Object> params) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        ensureRefundRecordTable();
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_record WHERE order_id = ? AND status = 0",
                Integer.class,
                orderId);
        if (existing != null && existing > 0) {
            log.info("退款申请幂等返回: orderId={}, userId={}, status={}", orderId, userId, order.getStatus());
            return;
        }

        if (!OrderConstant.STATUS_PAID.equals(order.getStatus()) &&
            !OrderConstant.STATUS_COMPLETED.equals(order.getStatus()) &&
            !OrderConstant.STATUS_SHIPPED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不允许申请售后");
        }

        String reason = stringParam(params, "reason", "用户申请退款");
        String images = stringParam(params, "images", null);
        Integer refundType = "return".equalsIgnoreCase(stringParam(params, "type", "refund")) ? 2 : 1;
        BigDecimal refundAmount = order.getPayAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款金额必须大于0");
        }

        order.setStatus(OrderConstant.STATUS_REFUNDING);
        order.setPaymentStatus(OrderConstant.STATUS_REFUNDING);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        jdbcTemplate.update("""
            INSERT INTO refund_record
                (order_id, order_no, user_id, refund_amount, refund_type, reason, images, status, apply_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0, NOW())
            """,
                order.getId(), order.getOrderNo(), order.getUserId(), refundAmount, refundType, reason, images);
        notifySellersRefundPending(order, refundAmount, reason);
        paymentService.createOrderRefund(order, refundAmount, reason);
    }

    /** 提交退货回寄运单 */
    @Transactional
    public void submitRefundReturnLogistics(Long orderId, Long userId, Map<String, Object> params) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        ensureRefundRecordTable();

        List<Map<String, Object>> records = jdbcTemplate.queryForList("""
            SELECT id, refund_type, status, return_tracking_no
            FROM refund_record
            WHERE order_id = ?
            ORDER BY id DESC
            LIMIT 1
            """, orderId);
        if (records.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "未找到退款申请记录");
        }

        Map<String, Object> record = records.get(0);
        int refundType = toInt(record.get("refund_type"));
        int refundStatus = toInt(record.get("status"));
        String existingTrackingNo = stringValue(record.get("return_tracking_no"));
        if (refundType != 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前售后类型无需提交退货运单");
        }
        if (!OrderConstant.STATUS_REFUNDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前订单状态不允许提交退货运单");
        }
        if (refundStatus == 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款申请已拒绝");
        }
        if (existingTrackingNo != null && !existingTrackingNo.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退货运单已提交");
        }

        String companyCode = stringParam(params, "companyCode", "");
        String companyName = stringParam(params, "companyName", "");
        String trackingNo = stringParam(params, "trackingNo", "");
        if (companyName == null || companyName.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择物流公司");
        }
        if (trackingNo == null || !trackingNo.matches("^[A-Za-z0-9-]{6,32}$")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请输入正确的运单号");
        }

        Long refundRecordId = ((Number) record.get("id")).longValue();
        jdbcTemplate.update("""
            UPDATE refund_record
            SET return_company_code = ?, return_company_name = ?, return_tracking_no = ?, return_status = 1, return_ship_time = NOW()
            WHERE id = ?
            """, companyCode, companyName, trackingNo, refundRecordId);
        notifySellersReturnShipment(order, companyName, trackingNo);
    }

    private void notifySellersRefundPending(Order order, BigDecimal refundAmount, String reason) {
        ensureMessagesTable();
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
        );
        if (orderItems.isEmpty()) {
            return;
        }

        String goodsTitle = orderItems.stream()
                .map(OrderItem::getTitle)
                .filter(Objects::nonNull)
                .filter(title -> !title.isBlank())
                .findFirst()
                .orElse("订单商品");
        String amountText = refundAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String safeReason = (reason == null || reason.isBlank()) ? "用户申请退款" : reason.trim();

        Map<Long, String> sellerPayloadMap = new LinkedHashMap<>();
        for (OrderItem item : orderItems) {
            Long sellerUserId = item.getArtistId();
            if (sellerUserId == null || sellerUserId.equals(order.getUserId()) || sellerPayloadMap.containsKey(sellerUserId)) {
                continue;
            }

            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put("orderId", order.getId());
            extra.put("orderNo", order.getOrderNo());
            extra.put("action", "refund_pending");
            extra.put("link", "/pages/order/list?type=sold&status=refund");
            sellerPayloadMap.put(sellerUserId, writeJsonSafely(extra));
        }

        for (Map.Entry<Long, String> entry : sellerPayloadMap.entrySet()) {
            jdbcTemplate.update("""
                INSERT INTO messages (user_id, type, title, content, data, is_read, create_time)
                VALUES (?, ?, ?, ?, ?, 0, NOW())
                """,
                    entry.getKey(),
                    "order",
                    "收到新的退款申请",
                    String.format("订单%s中的“%s”提交了退款申请，退款金额¥%s，原因：%s。",
                            order.getOrderNo(), goodsTitle, amountText, safeReason),
                    entry.getValue());
        }
    }

    private void ensureRefundRecordTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS refund_record (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                order_id BIGINT NOT NULL,
                order_no VARCHAR(64) DEFAULT NULL,
                user_id BIGINT NOT NULL,
                refund_amount DECIMAL(12,2) NOT NULL,
                refund_type TINYINT DEFAULT 1 COMMENT '1-仅退款 2-退货退款',
                reason VARCHAR(500) NOT NULL,
                images TEXT DEFAULT NULL,
                status TINYINT DEFAULT 0 COMMENT '0-待处理 1-同意 2-拒绝',
                handle_remark VARCHAR(255) DEFAULT NULL,
                return_company_code VARCHAR(32) DEFAULT NULL,
                return_company_name VARCHAR(64) DEFAULT NULL,
                return_tracking_no VARCHAR(64) DEFAULT NULL,
                return_status TINYINT DEFAULT NULL COMMENT '1-已寄回 2-运输中 3-派送中 4-已签收 5-拒收 6-退件',
                return_ship_time DATETIME DEFAULT NULL,
                return_receive_time DATETIME DEFAULT NULL,
                apply_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                handle_time DATETIME DEFAULT NULL,
                complete_time DATETIME DEFAULT NULL,
                KEY idx_order_id (order_id),
                KEY idx_user_id (user_id),
                KEY idx_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表'
            """);
        addColumnIfMissing("refund_record", "return_company_code", "VARCHAR(32) DEFAULT NULL COMMENT '退货回寄物流公司编码'");
        addColumnIfMissing("refund_record", "return_company_name", "VARCHAR(64) DEFAULT NULL COMMENT '退货回寄物流公司'");
        addColumnIfMissing("refund_record", "return_tracking_no", "VARCHAR(64) DEFAULT NULL COMMENT '退货回寄运单号'");
        addColumnIfMissing("refund_record", "return_status", "TINYINT DEFAULT NULL COMMENT '1-已寄回 2-运输中 3-派送中 4-已签收 5-拒收 6-退件'");
        addColumnIfMissing("refund_record", "return_ship_time", "DATETIME DEFAULT NULL COMMENT '退货回寄时间'");
        addColumnIfMissing("refund_record", "return_receive_time", "DATETIME DEFAULT NULL COMMENT '退货签收时间'");
    }

    private void ensureMessagesTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                type VARCHAR(32) NOT NULL,
                title VARCHAR(255) NOT NULL,
                content VARCHAR(1000) NOT NULL,
                data TEXT DEFAULT NULL,
                is_read TINYINT DEFAULT 0,
                read_time DATETIME DEFAULT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                KEY idx_user_type (user_id, type),
                KEY idx_user_read (user_id, is_read)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表'
            """);
    }

    private void notifySellersReturnShipment(Order order, String companyName, String trackingNo) {
        ensureMessagesTable();
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
        );
        if (orderItems.isEmpty()) {
            return;
        }

        String goodsTitle = orderItems.stream()
                .map(OrderItem::getTitle)
                .filter(Objects::nonNull)
                .filter(title -> !title.isBlank())
                .findFirst()
                .orElse("订单商品");

        Map<Long, String> sellerPayloadMap = new LinkedHashMap<>();
        for (OrderItem item : orderItems) {
            Long sellerUserId = item.getArtistId();
            if (sellerUserId == null || sellerUserId.equals(order.getUserId()) || sellerPayloadMap.containsKey(sellerUserId)) {
                continue;
            }

            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put("orderId", order.getId());
            extra.put("orderNo", order.getOrderNo());
            extra.put("action", "refund_return_tracking_submitted");
            extra.put("link", "/pages/order/list?type=sold&status=refund");
            sellerPayloadMap.put(sellerUserId, writeJsonSafely(extra));
        }

        for (Map.Entry<Long, String> entry : sellerPayloadMap.entrySet()) {
            jdbcTemplate.update("""
                INSERT INTO messages (user_id, type, title, content, data, is_read, create_time)
                VALUES (?, ?, ?, ?, ?, 0, NOW())
                """,
                    entry.getKey(),
                    "order",
                    "买家已提交退货运单",
                    String.format("订单%s中的“%s”已提交退货回寄运单，物流公司：%s，运单号：%s。",
                            order.getOrderNo(), goodsTitle, companyName, trackingNo),
                    entry.getValue());
        }
    }

    private boolean shouldAutoFinalizeSignedReturnRefund(Map<String, Object> refundRecord) {
        return toInt(refundRecord.get("refund_type")) == 2
            && toInt(refundRecord.get("status")) == 0
            && toInt(refundRecord.get("return_status")) == 4;
    }

    private void autoFinalizeSignedReturnRefund(Order order, Map<String, Object> refundRecord) {
        if (order == null || refundRecord == null) {
            return;
        }

        Long refundRecordId = toLong(refundRecord.get("id"));
        if (refundRecordId == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String reason = firstNonBlank(
            stringValue(refundRecord.get("reason")),
            "退货回寄已签收，系统自动退款"
        );
        BigDecimal refundAmount = decimalValue(refundRecord.get("refund_amount"));

        try {
            paymentService.ensurePaymentTables();
            RefundOrder refundOrder = paymentService.createOrderRefund(order, refundAmount, reason);
            String channel = stringValue(refundOrder.getChannel());
            String payNo = stringValue(refundOrder.getPayNo());
            String refundNo = stringValue(refundOrder.getRefundNo());

            if (payNo == null || payNo.isBlank()) {
                throw new IllegalStateException("缺少支付单号，无法发起自动退款");
            }

            jdbcTemplate.update(
                "UPDATE refund_order SET status = ?, request_payload = ?, update_time = ? WHERE refund_no = ?",
                PaymentService.STATUS_REFUNDING,
                "auto_signed_return_refund",
                now,
                refundNo
            );

            Map<String, ?> refundResult;
            if (PaymentService.CHANNEL_WECHAT.equalsIgnoreCase(channel)) {
                refundResult = wxPayService.refundWithResult(
                    payNo,
                    refundNo,
                    String.valueOf(toPaymentFen(refundOrder.getTotalAmount())),
                    String.valueOf(toPaymentFen(refundOrder.getRefundAmount())),
                    reason
                );
                if (!"SUCCESS".equals(refundResult.get("return_code")) || !"SUCCESS".equals(refundResult.get("result_code"))) {
                    throw new IllegalStateException(firstNonBlank(
                        stringValue(refundResult.get("err_code_des")),
                        firstNonBlank(stringValue(refundResult.get("return_msg")), "微信退款失败")
                    ));
                }
            } else if (PaymentService.CHANNEL_ALIPAY.equalsIgnoreCase(channel)) {
                refundResult = alipayService.refund(
                    payNo,
                    refundNo,
                    normalizeMoneyYuan(refundOrder.getRefundAmount()),
                    reason
                );
            } else {
                refundResult = Map.of("manual", "true", "message", "无渠道支付单，按自动退款完成");
            }

            Object channelRefundNo = refundResult.containsKey("refund_id")
                ? refundResult.get("refund_id")
                : (refundResult.containsKey("tradeNo") ? refundResult.get("tradeNo") : refundNo);

            paymentService.markRefundSuccessByBizNo(order.getOrderNo(), stringValue(channelRefundNo), refundResult);
            jdbcTemplate.update(
                """
                UPDATE refund_record
                SET status = 1,
                    handle_remark = ?,
                    handle_time = ?,
                    complete_time = ?,
                    return_receive_time = COALESCE(return_receive_time, ?)
                WHERE id = ? AND status = 0
                """,
                "退货回寄已签收，系统自动退款",
                now,
                now,
                now,
                refundRecordId
            );

            order.setStatus(OrderConstant.STATUS_REFUNDED);
            order.setPaymentStatus(OrderConstant.STATUS_REFUNDED);
            order.setUpdateTime(now);
            orderMapper.updateById(order);
        } catch (Exception e) {
            log.warn("订单{}退货签收后自动退款失败: {}", order.getId(), e.getMessage());
        }
    }

    private String writeJsonSafely(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化站内消息附加数据失败: {}", e.getMessage());
            return null;
        }
    }

    private String stringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || params.get(key) == null) {
            return defaultValue;
        }
        String value = String.valueOf(params.get(key)).trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private BigDecimal decimalParam(Map<String, Object> params, String key, BigDecimal defaultValue) {
        if (params == null || params.get(key) == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(String.valueOf(params.get(key)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** 微信支付统一下单 */
    public String unifiedOrder(Long orderId, Long userId) {
        return unifiedOrder(orderId, userId, null);
    }

    /** 微信支付统一下单 (支持支付方式) */
    public String unifiedOrder(Long orderId, Long userId, String openId) {
        return unifiedOrder(orderId, userId, openId, "mini");
    }

    public String unifiedOrder(Long orderId, Long userId, String openId, String payScene) {
        validateWechatPayRequest(openId, payScene);
        Order order = getPayableOrder(orderId, userId);

        long totalAmount = toPaymentFen(order.getPayAmount());
        
        // 商品描述
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        String description = items.isEmpty() ? "艺术品购买" : items.get(0).getTitle();
        if (description.length() > 50) {
            description = description.substring(0, 47) + "...";
        }

        try {
            String tradeType = (openId != null && !openId.isEmpty()) ? "WECHAT_JSAPI" : "WECHAT_NATIVE";
            PaymentOrder payment = paymentService.createOrderPayment(order, PaymentService.CHANNEL_WECHAT, tradeType, description);
            String codeUrl;
            
            if (openId != null && !openId.isEmpty()) {
                // JSAPI支付 (小程序/公众号)
                Map<String, String> jsApiResult = wxPayService.unifiedOrderJsApi(
                        payment.getPayNo(),
                        String.valueOf(totalAmount), 
                        openId, 
                        description,
                        payScene
                );
                codeUrl = jsApiResult.get("prepay_id");
                paymentService.markPaying(payment.getPayNo(), jsApiResult);
            } else {
                // Native支付 (二维码支付)
                codeUrl = wxPayService.unifiedOrderNative(
                        payment.getPayNo(),
                        String.valueOf(totalAmount), 
                        description
                );
                paymentService.markPaying(payment.getPayNo(), Map.of("code_url", codeUrl));
            }
            
            // 存入Redis，设置支付过期时间（30分钟）
            redisTemplate.opsForValue().set("pay:order:" + orderId, payment.getPayNo(), 30, TimeUnit.MINUTES);
            
            log.info("微信支付下单成功 - 订单ID: {}, OrderNo: {}, PayNo: {}, codeUrl: {}",
                    orderId, order.getOrderNo(), payment.getPayNo(), codeUrl);
            
            return codeUrl;
            
        } catch (Exception e) {
            log.error("微信支付统一下单失败", e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付下单失败: " + e.getMessage());
        }
    }

    /** 微信支付统一下单 - 返回完整支付参数 */
    public Map<String, Object> unifiedOrderWithParams(Long orderId, Long userId, String openId) {
        return unifiedOrderWithParams(orderId, userId, openId, "mini");
    }

    public Map<String, Object> unifiedOrderWithParams(Long orderId, Long userId, String openId, String payScene) {
        validateWechatPayRequest(openId, payScene);
        Order order = getPayableOrder(orderId, userId);

        long totalAmount = toPaymentFen(order.getPayAmount());
        
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        String description = items.isEmpty() ? "艺术品购买" : items.get(0).getTitle();

        try {
            boolean appScene = "app".equalsIgnoreCase(payScene);
            PaymentOrder payment = paymentService.createOrderPayment(
                    order,
                    PaymentService.CHANNEL_WECHAT,
                    appScene ? "WECHAT_APP" : "WECHAT_JSAPI",
                    description);
            Map<String, String> payResult = appScene
                    ? wxPayService.unifiedOrderApp(
                            payment.getPayNo(),
                            String.valueOf(totalAmount),
                            description
                    )
                    : wxPayService.unifiedOrderJsApi(
                            payment.getPayNo(),
                            String.valueOf(totalAmount),
                            openId,
                            description,
                            payScene
                    );
            
            paymentService.markPaying(payment.getPayNo(), payResult);
            redisTemplate.opsForValue().set("pay:order:" + orderId, payment.getPayNo(), 30, TimeUnit.MINUTES);
            
            Map<String, Object> payParams = new HashMap<>(payResult);
            payParams.put("order_no", order.getOrderNo());
            payParams.put("pay_no", payment.getPayNo());
            payParams.put("pay_amount", order.getPayAmount());
            payParams.put("description", description);
            
            return payParams;
            
        } catch (Exception e) {
            log.error("微信支付统一下单失败", e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付下单失败: " + e.getMessage());
        }
    }

    private void validateWechatPayRequest(String openId, String payScene) {
        boolean appScene = "app".equalsIgnoreCase(payScene);
        if (!appScene && (openId == null || openId.isBlank())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "缺少微信身份信息，请重新登录后再试");
        }
        if (!appScene && openId.startsWith("mock_openid_")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前H5登录态不支持微信支付，请使用支付宝或微信小程序");
        }
        String sceneAppId;
        if (appScene) {
            sceneAppId = wxPayConfig.getAppId();
        } else if ("h5".equalsIgnoreCase(payScene) || "official".equalsIgnoreCase(payScene)) {
            sceneAppId = firstNonBlank(wxPayConfig.getOfficialAppId(), wxPayConfig.getAppId());
        } else {
            sceneAppId = firstNonBlank(wxPayConfig.getMiniAppId(), wxPayConfig.getAppId());
        }
        if (isPlaceholder(sceneAppId) || isPlaceholder(wxPayConfig.getMchId())
                || isPlaceholder(wxPayConfig.getApiKey()) || isPlaceholder(wxPayConfig.getMchKey())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "微信支付商户配置未完成，请联系管理员");
        }
    }

    private String firstNonBlank(String first, String fallback) {
        return (first != null && !first.isBlank()) ? first : fallback;
    }

    private boolean isPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("your_app_id")
                || normalized.contains("your_mch_id")
                || normalized.contains("your_mch_key")
                || normalized.contains("your_api_key")
                || normalized.contains("placeholder");
    }

    /** 查询支付状态 */
    public Map<String, String> queryPayStatus(String orderNo) {
        try {
            PaymentOrder payment = paymentService.findLatestByBizNo(orderNo);
            String outTradeNo = payment != null ? payment.getPayNo() : orderNo;
            Map<String, String> result = wxPayService.queryOrder(outTradeNo);
            
            Map<String, String> response = new HashMap<>();
            response.put("trade_state", result.get("trade_state"));
            response.put("trade_state_desc", result.get("trade_state_desc"));
            response.put("transaction_id", result.get("transaction_id"));
            response.put("total_fee", result.get("total_fee"));
            response.put("pay_no", outTradeNo);
            response.put("order_no", orderNo);
            
            return response;
        } catch (Exception e) {
            log.error("查询支付状态失败", e);
            Map<String, String> response = new HashMap<>();
            response.put("trade_state", "ERROR");
            response.put("trade_state_desc", "查询失败");
            return response;
        }
    }

    /** 支付宝手机网站支付下单。 */
    public Map<String, Object> createAlipayWapPay(Long orderId, Long userId) {
        return createAlipayWapPay(orderId, userId, null);
    }

    /** 支付宝手机网站支付下单。 */
    public Map<String, Object> createAlipayWapPay(Long orderId, Long userId, String returnScene) {
        Order order = getPayableOrder(orderId, userId);
        BigDecimal amountYuan = normalizeMoneyYuan(order.getPayAmount());
        String description = getOrderDescription(orderId);

        try {
            PaymentOrder payment = paymentService.createOrderPayment(order, PaymentService.CHANNEL_ALIPAY, "ALIPAY_WAP", description);
            Map<String, Object> payParams = new HashMap<>(alipayService.createWapPay(
                    payment.getPayNo(),
                    amountYuan,
                    description,
                    buildAlipayReturnUrl(orderId, returnScene)
            ));
            paymentService.markPaying(payment.getPayNo(), payParams);
            payParams.put("description", description);
            payParams.put("biz_order_no", order.getOrderNo());
            payParams.put("pay_no", payment.getPayNo());
            redisTemplate.opsForValue().set("pay:order:" + orderId, payment.getPayNo(), 30, TimeUnit.MINUTES);
            log.info("支付宝下单成功 - 订单ID: {}, OrderNo: {}, PayNo: {}", orderId, order.getOrderNo(), payment.getPayNo());
            return payParams;
        } catch (Exception e) {
            log.error("支付宝下单失败", e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付宝下单失败: " + e.getMessage());
        }
    }

    /** 支付宝 App 支付下单。 */
    public Map<String, Object> createAlipayAppPay(Long orderId, Long userId) {
        Order order = getPayableOrder(orderId, userId);
        BigDecimal amountYuan = normalizeMoneyYuan(order.getPayAmount());
        String description = getOrderDescription(orderId);

        try {
            PaymentOrder payment = paymentService.createOrderPayment(order, PaymentService.CHANNEL_ALIPAY, "ALIPAY_APP", description);
            Map<String, Object> payParams = new HashMap<>(alipayService.createAppPay(
                    payment.getPayNo(),
                    amountYuan,
                    description
            ));
            paymentService.markPaying(payment.getPayNo(), payParams);
            payParams.put("description", description);
            payParams.put("biz_order_no", order.getOrderNo());
            payParams.put("pay_no", payment.getPayNo());
            redisTemplate.opsForValue().set("pay:order:" + orderId, payment.getPayNo(), 30, TimeUnit.MINUTES);
            log.info("支付宝App下单成功 - 订单ID: {}, OrderNo: {}, PayNo: {}", orderId, order.getOrderNo(), payment.getPayNo());
            return payParams;
        } catch (Exception e) {
            log.error("支付宝App下单失败", e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付宝App下单失败: " + e.getMessage());
        }
    }

    private String buildAlipayReturnUrl(Long orderId, String returnScene) {
        if ("ios-app".equalsIgnoreCase(String.valueOf(returnScene))) {
            return "https://a.art1.cn/app/pay-result?orderId=" + orderId
                    + "&paymentMethod=alipay&checkPay=1";
        }
        return "https://a.art1.cn/#/pages/order/pay?orderId=" + orderId
                + "&paymentMethod=alipay&checkPay=1";
    }

    private BigDecimal normalizeMoneyYuan(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private long toPaymentFen(BigDecimal amountYuan) {
        return normalizeMoneyYuan(amountYuan)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private Order getPayableOrder(Long orderId, Long userId) {
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
        if (order.getPayExpireTime() != null && !order.getPayExpireTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.PAYMENT_TIMEOUT, "订单已超过支付期限，请重新下单");
        }
        return order;
    }

    private String getOrderDescription(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        String description = items.isEmpty() ? "艺术品购买" : items.get(0).getTitle();
        if (description == null || description.isBlank()) {
            description = "艺术品购买";
        }
        return description.length() > 50 ? description.substring(0, 47) + "..." : description;
    }

    /** 本地开发模拟支付成功：复用真实支付回调处理链路。 */
    @Transactional(rollbackFor = Exception.class)
    public void mockPaySuccess(Long orderId, Long userId) {
        Order order = getOrderById(orderId, userId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!OrderConstant.STATUS_PENDING_PAYMENT.equals(order.getStatus())
                && !isPaidLikeStatus(order.getStatus())) {
            return;
        }
        handlePayCallback(order.getOrderNo(), "MOCK-" + order.getOrderNo());
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
            if (isPaidLikeStatus(order.getStatus()) && !OrderConstant.SOURCE_RESALE.equals(order.getSource())
                    && !hasAnyOrderSaleBill(order.getId())) {
                log.warn("订单 {} 已支付但缺少作品销售入账流水，执行补偿结算", orderNo);
                processPaidOrder(order);
            }
            log.info("订单 {} 已处理，幂等返回，当前状态: {}", orderNo, order.getStatus());
            return;
        }

        LocalDateTime paidAt = LocalDateTime.now();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderConstant.STATUS_PENDING_PAYMENT)
                .set(Order::getStatus, OrderConstant.STATUS_PAID)
                .set(Order::getPaymentStatus, OrderConstant.STATUS_PAID)
                .set(Order::getPayTime, paidAt)
                .set(Order::getUpdateTime, paidAt));
        if (updated != 1) {
            log.info("订单 {} 已由其他回调处理", orderNo);
            return;
        }
        order.setStatus(OrderConstant.STATUS_PAID);
        order.setPaymentStatus(OrderConstant.STATUS_PAID);
        order.setPayTime(paidAt);
        order.setUpdateTime(paidAt);

        processPaidOrder(order);
    }

    private void processPaidOrder(Order order) {
        String orderNo = order.getOrderNo();

        // === 转售订单：发布事件标记已支付（异步处理，不阻塞本地事务） ===
        if (OrderConstant.SOURCE_RESALE.equals(order.getSource())) {
            Long resaleId = parseResaleIdFromRemark(order.getRemark());
            if (resaleId != null) {
                boolean synced = resaleRestClient.markAsPaid(resaleId, order.getUserId());
                if (!synced) {
                    log.warn("转售订单支付后同步转售状态失败，将保留订单已支付: orderId={}, resaleId={}",
                            order.getId(), resaleId);
                }
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

        // === 普通订单：全平台作品按系统配置扣除平台抽佣，再给艺术家结算收益 ===
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        markOrderArtworksSold(order, items);
        BigDecimal platformCommissionAmount = calculatePrimarySalePlatformFee(order);
        BigDecimal totalItemAmount = items.stream()
                .map(this::orderItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long platformWalletUserId = resolvePlatformWalletUserId();
        if (platformCommissionAmount.compareTo(BigDecimal.ZERO) > 0 && platformWalletUserId != null) {
            if (!hasWalletBill(platformWalletUserId, "platform_fee", order.getId(), "order", platformCommissionAmount)) {
                financeEventPublisher.publish(FinanceEvent.builder()
                        .type(FinanceEventType.PLATFORM_FEE)
                        .platformWalletUserId(platformWalletUserId)
                        .amount(platformCommissionAmount)
                        .relatedId(order.getId())
                        .relatedType("order")
                        .orderNo(orderNo)
                        .remark("普通订单平台抽佣 " + orderNo)
                        .build());
                boolean settled = walletClient.income(platformWalletUserId, platformCommissionAmount, "platform_fee",
                        order.getId(), "order", "普通订单平台抽佣 " + orderNo);
                if (!settled) {
                    log.error("普通订单平台抽佣入账失败: orderId={}, userId={}, amount={}",
                            order.getId(), platformWalletUserId, platformCommissionAmount);
                }
            }
        }
        for (OrderItem item : items) {
            Artwork artwork = artworkMapper.selectById(item.getArtworkId());
            BigDecimal itemSubtotal = orderItemSubtotal(item);
            if (artwork != null && artwork.getAuthorId() != null && itemSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal itemPlatformFee = allocatePlatformFee(platformCommissionAmount, itemSubtotal, totalItemAmount);
                BigDecimal artistIncome = itemSubtotal.subtract(itemPlatformFee).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                if (hasWalletBill(artwork.getAuthorId(), "order_sale", order.getId(), "order", artistIncome)) {
                    continue;
                }
                financeEventPublisher.publish(FinanceEvent.builder()
                        .type(FinanceEventType.ARTIST_INCOME)
                        .userId(artwork.getAuthorId())
                        .amount(artistIncome)
                        .relatedId(order.getId())
                        .relatedType("order")
                        .remark("作品销售: " + artwork.getTitle() + " " + orderNo)
                        .build());
                boolean settled = walletClient.frozenIncome(artwork.getAuthorId(), artistIncome, "order_sale",
                        order.getId(), "order",
                        "作品销售: " + artwork.getTitle() + " " + orderNo);
                if (!settled) {
                    log.error("作品销售冻结入账失败: orderId={}, artworkId={}, userId={}, amount={}",
                            order.getId(), item.getArtworkId(), artwork.getAuthorId(), artistIncome);
                }
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
                    .buyerUserId(order.getUserId())
                    .build());
            commissionRestClient.settleCommission(order.getId(), orderNo, order.getPayAmount(),
                    order.getUserId(), promoterId, artworkId);
        }
    }

    private BigDecimal calculatePrimarySalePlatformFee(Order order) {
        if (order == null || !isPlatformCommissionEnabled()) {
            return BigDecimal.ZERO;
        }
        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = resolveRate(PLATFORM_COMMISSION_PRIMARY_RATE_KEY, BigDecimal.ZERO);
        BigDecimal fee = payAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal minFee = resolveAmount(PLATFORM_COMMISSION_MIN_FEE_KEY, BigDecimal.ZERO);
        if (fee.compareTo(BigDecimal.ZERO) > 0 && fee.compareTo(minFee) < 0) {
            fee = minFee;
        }
        if (fee.compareTo(payAmount) > 0) {
            fee = payAmount;
        }
        return fee;
    }

    private BigDecimal orderItemSubtotal(OrderItem item) {
        if (item == null || item.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        int quantity = Math.max(item.getQuantity() == null ? 1 : item.getQuantity(), 1);
        return item.getPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPaidLikeStatus(String status) {
        return OrderConstant.STATUS_PAID.equals(status)
                || OrderConstant.STATUS_SHIPPED.equals(status)
                || OrderConstant.STATUS_COMPLETED.equals(status);
    }

    private boolean hasAnyOrderSaleBill(Long orderId) {
        if (orderId == null) {
            return false;
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM wallet_bill WHERE related_id = ? AND related_type = 'order' AND bill_type = 'order_sale'",
                    Integer.class,
                    orderId
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("检查订单销售入账流水失败: orderId={}, error={}", orderId, e.getMessage());
            return false;
        }
    }

    private void releaseFrozenOrderSale(Order order) {
        if (order == null || order.getId() == null || OrderConstant.SOURCE_RESALE.equals(order.getSource())) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        BigDecimal platformCommissionAmount = calculatePrimarySalePlatformFee(order);
        BigDecimal totalItemAmount = items.stream()
                .map(this::orderItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (OrderItem item : items) {
            Artwork artwork = artworkMapper.selectById(item.getArtworkId());
            BigDecimal itemSubtotal = orderItemSubtotal(item);
            if (artwork == null || artwork.getAuthorId() == null || itemSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal itemPlatformFee = allocatePlatformFee(platformCommissionAmount, itemSubtotal, totalItemAmount);
            BigDecimal artistIncome = itemSubtotal.subtract(itemPlatformFee).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            if (artistIncome.compareTo(BigDecimal.ZERO) <= 0
                    || hasWalletBill(artwork.getAuthorId(), "order_sale_release", order.getId(), "order", artistIncome)) {
                continue;
            }
            boolean released = walletClient.releaseFrozenIncome(artwork.getAuthorId(), artistIncome,
                    "order_sale_release", order.getId(), "order",
                    "作品销售确认收货: " + artwork.getTitle() + " " + order.getOrderNo());
            if (!released) {
                log.error("作品销售解冻失败: orderId={}, artworkId={}, userId={}, amount={}",
                        order.getId(), item.getArtworkId(), artwork.getAuthorId(), artistIncome);
            }
        }
    }

    private boolean hasWalletBill(Long userId, String billType, Long relatedId, String relatedType, BigDecimal amount) {
        if (userId == null || billType == null || relatedId == null || amount == null) {
            return false;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM wallet_bill
                    WHERE user_id = ?
                      AND bill_type = ?
                      AND related_id = ?
                      AND related_type = ?
                      AND amount = ?
                    """,
                    Integer.class,
                    userId, billType, relatedId, relatedType, amount
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("检查钱包流水失败: userId={}, billType={}, relatedId={}, error={}",
                    userId, billType, relatedId, e.getMessage());
            return false;
        }
    }

    private BigDecimal allocatePlatformFee(BigDecimal totalPlatformFee, BigDecimal itemSubtotal, BigDecimal totalItemAmount) {
        if (totalPlatformFee == null || itemSubtotal == null || totalItemAmount == null
                || totalPlatformFee.compareTo(BigDecimal.ZERO) <= 0
                || itemSubtotal.compareTo(BigDecimal.ZERO) <= 0
                || totalItemAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalPlatformFee.multiply(itemSubtotal)
                .divide(totalItemAmount, 2, RoundingMode.HALF_UP)
                .min(itemSubtotal);
    }

    private boolean isPlatformCommissionEnabled() {
        String raw = readConfigValue(PLATFORM_COMMISSION_ENABLED_KEY);
        return raw == null || raw.isBlank() || Boolean.parseBoolean(raw.trim());
    }

    private BigDecimal resolveRate(String key, BigDecimal fallbackRate) {
        BigDecimal percent = resolveAmount(key, null);
        if (percent != null) {
            return percent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return fallbackRate != null ? fallbackRate : BigDecimal.ZERO;
    }

    private BigDecimal resolveAmount(String key, BigDecimal fallback) {
        String raw = readConfigValue(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            log.warn("平台抽佣配置解析失败: key={}, value={}", key, raw);
            return fallback;
        }
    }

    private Long resolvePlatformWalletUserId() {
        String walletUid = readConfigValue(PLATFORM_COMMISSION_WALLET_UID_KEY);
        if (walletUid == null || walletUid.isBlank()) {
            return platformWalletUserId;
        }
        String trimmed = walletUid.trim();
        for (String table : List.of("users", "user_account", "sys_user")) {
            for (String column : List.of("uid", "user_uid")) {
                Long userId = queryPlatformWalletUserId(table, column, trimmed);
                if (userId != null) {
                    return userId;
                }
            }
        }
        log.warn("平台钱包UID未匹配用户，回退配置文件用户ID: uid={}", trimmed);
        return platformWalletUserId;
    }

    private Long queryPlatformWalletUserId(String table, String column, String walletUid) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM " + table + " WHERE " + column + " = ? LIMIT 1",
                    Long.class,
                    walletUid
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readConfigValue(String key) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT config_value FROM system_config WHERE config_key = ? LIMIT 1",
                    String.class,
                    key
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 支付成功后确认预占库存，并更新作品成交信息。历史未预占订单走兼容扣减。 */
    private void markOrderArtworksSold(Order order, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (OrderItem item : items) {
            Artwork artwork = artworkMapper.selectById(item.getArtworkId());
            if (artwork == null) {
                continue;
            }
            int quantity = Math.max(item.getQuantity() == null ? 1 : item.getQuantity(), 1);
            BigDecimal settledPrice = item.getPrice();
            BigDecimal basePrice = artwork.getOriginalPrice() != null
                    && artwork.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0
                    ? artwork.getOriginalPrice()
                    : artwork.getPrice();

            boolean reserved = confirmStockReservation(order.getId(), artwork.getId());
            if (!reserved) {
                reserveLegacyPaidOrderStock(artwork, quantity);
            }
            LambdaUpdateWrapper<Artwork> update = new LambdaUpdateWrapper<Artwork>()
                    .eq(Artwork::getId, artwork.getId())
                    .setSql("status = CASE WHEN COALESCE(stock, 0) <= 0 THEN "
                            + ProductConstant.STATUS_SOLD_OUT + " ELSE " + ProductConstant.STATUS_ON_SALE + " END")
                    .setSql("holder_id = CASE WHEN COALESCE(stock, 0) <= 0 THEN " + order.getUserId() + " ELSE holder_id END")
                    .setSql("holder_since = CASE WHEN COALESCE(stock, 0) <= 0 THEN CURRENT_TIMESTAMP ELSE holder_since END")
                    .setSql("sale_count = COALESCE(sale_count, 0) + " + quantity);

            if (item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                update.set(Artwork::getPrice, settledPrice);
                if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0) {
                    update.set(Artwork::getPriceRise, settledPrice
                            .divide(basePrice, 6, RoundingMode.HALF_UP)
                            .subtract(BigDecimal.ONE)
                            .setScale(4, RoundingMode.HALF_UP));
                }
            }

            int updated = artworkMapper.update(null, update);
            if (updated != 1) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH,
                        "作品【" + artwork.getTitle() + "】已售出或库存不足");
            }
        }
    }

    private boolean confirmStockReservation(Long orderId, Long artworkId) {
        int confirmed = jdbcTemplate.update("""
                UPDATE order_stock_reservation
                SET status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE order_id = ? AND artwork_id = ? AND status = ?
                """, RESERVATION_CONFIRMED, orderId, artworkId, RESERVATION_RESERVED);
        if (confirmed == 1) {
            return true;
        }
        List<String> statuses = jdbcTemplate.queryForList("""
                SELECT status FROM order_stock_reservation
                WHERE order_id = ? AND artwork_id = ?
                """, String.class, orderId, artworkId);
        if (statuses.isEmpty()) {
            return false;
        }
        if (RESERVATION_CONFIRMED.equals(statuses.get(0))) {
            return true;
        }
        throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, "订单库存预占已释放，请重新下单");
    }

    private void reserveLegacyPaidOrderStock(Artwork artwork, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE artwork
                SET status = CASE
                        WHEN stock IS NULL OR stock <= 0 OR stock - ? <= 0 THEN ?
                        ELSE status
                    END,
                    stock = CASE
                        WHEN stock IS NULL OR stock <= 0 THEN 0
                        ELSE stock - ?
                    END,
                    update_time = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                  AND status = ?
                  AND (((stock IS NULL OR stock <= 0) AND ? = 1) OR stock >= ?)
                """, quantity, ProductConstant.STATUS_SOLD_OUT, quantity,
                artwork.getId(), ProductConstant.STATUS_ON_SALE, quantity, quantity);
        if (updated != 1) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH,
                    "作品【" + artwork.getTitle() + "】已售出或库存不足");
        }
    }

    private void finalizeZeroAmountOrder(Order order) {
        if (order == null || order.getOrderNo() == null) {
            return;
        }
        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;
        if (payAmount.compareTo(BigDecimal.ZERO) > 0 || !OrderConstant.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderConstant.STATUS_PAID);
        order.setPaymentStatus(OrderConstant.STATUS_PAID);
        order.setPayTime(now);
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        processPaidOrder(order);
    }

    private void normalizeZeroAmountPendingOrders(Long userId) {
        if (userId == null) {
            return;
        }
        List<Order> zeroAmountOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getStatus, OrderConstant.STATUS_PENDING_PAYMENT)
                        .and(wrapper -> wrapper.isNull(Order::getPayAmount).or().le(Order::getPayAmount, BigDecimal.ZERO))
        );
        for (Order order : zeroAmountOrders) {
            finalizeZeroAmountOrder(order);
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
        Logistics logistics = null;
        try {
            logistics = logisticsMapper.selectOne(
                    new LambdaQueryWrapper<Logistics>()
                            .eq(Logistics::getOrderId, order.getId())
                            .orderByDesc(Logistics::getCreateTime)
                            .last("LIMIT 1")
            );
        } catch (Exception ex) {
            log.warn("订单{}物流信息读取失败，继续返回订单详情", order.getId(), ex);
        }
        if (logistics != null) {
            vo.setTrackingNo(logistics.getTrackingNo());
            vo.setExpressName(logistics.getCompanyName());
            if (logistics.getShipTime() != null) {
                vo.setShipTime(logistics.getShipTime().toString());
            }
            if (logistics.getReceiveTime() != null) {
                vo.setReceiveTime(logistics.getReceiveTime().toString());
            }
        } else {
            vo.setTrackingNo(order.getTrackingNo());
            vo.setExpressName(order.getExpressName());
        }

        ensureRefundRecordTable();
        List<Map<String, Object>> refundRecords = jdbcTemplate.queryForList("""
            SELECT id, refund_amount, refund_type, reason, images, status,
                   return_company_code, return_company_name, return_tracking_no, return_status,
                   return_ship_time, return_receive_time
            FROM refund_record
            WHERE order_id = ?
            ORDER BY id DESC
            LIMIT 1
            """, order.getId());
        if (!refundRecords.isEmpty()) {
            Map<String, Object> refund = refundRecords.get(0);
            if (shouldAutoFinalizeSignedReturnRefund(refund)) {
                autoFinalizeSignedReturnRefund(order, refund);
                order = orderMapper.selectById(order.getId());
                refundRecords = jdbcTemplate.queryForList("""
                    SELECT id, refund_amount, refund_type, reason, images, status,
                           return_company_code, return_company_name, return_tracking_no, return_status,
                           return_ship_time, return_receive_time
                    FROM refund_record
                    WHERE order_id = ?
                    ORDER BY id DESC
                    LIMIT 1
                    """, order.getId());
                if (!refundRecords.isEmpty()) {
                    refund = refundRecords.get(0);
                }
            }
            vo.setRefundType(toIntObject(refund.get("refund_type")));
            vo.setRefundStatus(toIntObject(refund.get("status")));
            vo.setRefundReason(stringValue(refund.get("reason")));
            vo.setRefundImages(stringValue(refund.get("images")));
            vo.setRefundAmount(decimalValue(refund.get("refund_amount")));
            vo.setReturnCompanyCode(stringValue(refund.get("return_company_code")));
            vo.setReturnCompanyName(stringValue(refund.get("return_company_name")));
            vo.setReturnTrackingNo(stringValue(refund.get("return_tracking_no")));
            vo.setReturnStatus(toIntObject(refund.get("return_status")));
            Object returnShipTime = refund.get("return_ship_time");
            vo.setReturnShipTime(returnShipTime != null ? String.valueOf(returnShipTime) : null);
            Object returnReceiveTime = refund.get("return_receive_time");
            vo.setReturnReceiveTime(returnReceiveTime != null ? String.valueOf(returnReceiveTime) : null);
        }

        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime() != null ? order.getPayTime().toString() : null);
        vo.setShipTime(order.getShipTime() != null ? order.getShipTime().toString() : null);
        vo.setReceiveTime(order.getReceiveTime() != null ? order.getReceiveTime().toString() : null);
        vo.setSourceText(getSourceText(order.getSource()));
        vo.setStatusText(getStatusText(order.getStatus()));
        vo.setBuyerUserId(order.getUserId());
        if (order.getUserId() != null) {
            User buyer = userMapper.selectById(order.getUserId());
            if (buyer != null) {
                vo.setBuyerName(buyer.getNickname());
                vo.setBuyerAvatar(buyer.getAvatar());
            }
        }

        // 卖家信息（从第一个订单项的作者获取）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        
        // 如果订单没有卖家信息，优先使用订单上的卖家ID回填
        if ((order.getSellerName() == null || order.getSellerName().isEmpty())) {
            Long sellerUserId = order.getSellerUserId();
            if (sellerUserId == null && !items.isEmpty()) {
                sellerUserId = items.get(0).getArtistId();
            }
            if (sellerUserId != null) {
                User seller = userMapper.selectById(sellerUserId);
                if (seller != null) {
                    order.setSellerUserId(sellerUserId);
                    order.setSellerName(seller.getNickname());
                    order.setSellerAvatar(seller.getAvatar());
                }
            }
        }
        
        final String orderSource = order.getSource();
        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setArtworkId(item.getArtworkId());
            itemVO.setTitle(item.getTitle());
            itemVO.setCoverImage(item.getCoverImage());
            itemVO.setAuthorName(item.getAuthorName());
            itemVO.setArtistName(item.getAuthorName());
            Artwork artwork = item.getArtworkId() != null ? artworkMapper.selectById(item.getArtworkId()) : null;
            BigDecimal resolvedPrice = item.getPrice();
            if (!OrderConstant.SOURCE_RESALE.equals(orderSource)) {
                BigDecimal basePrice = getArtworkBasePrice(artwork);
                resolvedPrice = normalizeArtworkAmountScale(resolvedPrice, basePrice, 1, true);
            }
            itemVO.setPrice(resolvedPrice != null ? resolvedPrice : BigDecimal.ZERO);
            itemVO.setQuantity(item.getQuantity());
            BigDecimal resolvedSubtotal = item.getSubtotal();
            if ((resolvedSubtotal == null || resolvedSubtotal.compareTo(BigDecimal.ZERO) <= 0)
                    && resolvedPrice != null && resolvedPrice.compareTo(BigDecimal.ZERO) > 0) {
                resolvedSubtotal = resolvedPrice.multiply(BigDecimal.valueOf(
                        item.getQuantity() == null || item.getQuantity() <= 0 ? 1 : item.getQuantity()
                ));
            }
            if (!OrderConstant.SOURCE_RESALE.equals(orderSource)) {
                BigDecimal basePrice = getArtworkBasePrice(artwork);
                resolvedSubtotal = normalizeArtworkAmountScale(
                        resolvedSubtotal,
                        basePrice,
                        item.getQuantity() == null ? 1 : item.getQuantity(),
                        true
                );
            }
            itemVO.setSubtotal(resolvedSubtotal != null ? resolvedSubtotal : BigDecimal.ZERO);
            if (artwork != null) {
                if (itemVO.getTitle() == null || itemVO.getTitle().isEmpty()) {
                    itemVO.setTitle(artwork.getTitle());
                }
                if (itemVO.getCoverImage() == null || itemVO.getCoverImage().isEmpty()) {
                    itemVO.setCoverImage(artwork.getCoverImage());
                }
                itemVO.setArtType(artwork.getArtType());
                itemVO.setMaterial(artwork.getMedium());
                itemVO.setSize(artwork.getSize());
                itemVO.setYear(artwork.getYear());
                itemVO.setSpecName(buildArtworkSpec(artwork));
                if (itemVO.getAuthorName() == null || itemVO.getAuthorName().isBlank()) {
                    User artist = artwork.getAuthorId() != null ? userMapper.selectById(artwork.getAuthorId()) : null;
                    if (artist != null) {
                        itemVO.setAuthorName(artist.getNickname());
                        itemVO.setArtistName(artist.getNickname());
                    }
                }
            }
            return itemVO;
        }).collect(Collectors.toList());

        vo.setItems(itemVOs);

        BigDecimal derivedGoodsAmount = itemVOs.stream()
                .map(OrderItemVO::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (vo.getTotalAmount() == null || vo.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            vo.setTotalAmount(derivedGoodsAmount);
        } else if (derivedGoodsAmount.compareTo(BigDecimal.ZERO) > 0
                && vo.getTotalAmount().compareTo(derivedGoodsAmount.multiply(BigDecimal.TEN)) > 0) {
            vo.setTotalAmount(derivedGoodsAmount);
        }
        if (vo.getPayAmount() == null || vo.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal freight = vo.getFreight() != null ? vo.getFreight() : BigDecimal.ZERO;
            BigDecimal discount = vo.getDiscountAmount() != null ? vo.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal derivedPayAmount = derivedGoodsAmount.add(freight).subtract(discount);
            vo.setPayAmount(derivedPayAmount.max(BigDecimal.ZERO));
        } else {
            BigDecimal freight = vo.getFreight() != null ? vo.getFreight() : BigDecimal.ZERO;
            BigDecimal discount = vo.getDiscountAmount() != null ? vo.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal derivedPayAmount = derivedGoodsAmount.add(freight).subtract(discount).max(BigDecimal.ZERO);
            if (derivedPayAmount.compareTo(BigDecimal.ZERO) > 0
                    && vo.getPayAmount().compareTo(derivedPayAmount.multiply(BigDecimal.TEN)) > 0) {
                vo.setPayAmount(derivedPayAmount);
            }
        }

        // 设置卖家信息到VO
        vo.setSellerName(order.getSellerName());
        vo.setSellerAvatar(order.getSellerAvatar());

        applyFrontendFenAmounts(vo);
        return vo;
    }

    /**
     * 兼容已发布 APP/H5 订单页面：订单接口金额字段仍按“分”返回。
     * 数据库和支付通道内部继续按“元”处理，避免影响真实支付金额。
     */
    private void applyFrontendFenAmounts(OrderVO vo) {
        if (vo == null) {
            return;
        }
        vo.setTotalAmount(toFrontendFen(vo.getTotalAmount()));
        vo.setPayAmount(toFrontendFen(vo.getPayAmount()));
        vo.setDiscountAmount(toFrontendFen(vo.getDiscountAmount()));
        vo.setFreight(toFrontendFen(vo.getFreight()));
        vo.setRefundAmount(toFrontendFen(vo.getRefundAmount()));
        if (vo.getItems() == null) {
            return;
        }
        for (OrderItemVO item : vo.getItems()) {
            item.setPrice(toFrontendFen(item.getPrice()));
            item.setSubtotal(toFrontendFen(item.getSubtotal()));
        }
    }

    private BigDecimal toFrontendFen(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer toIntObject(Object value) {
        return value == null ? null : toInt(value);
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(String.valueOf(value).trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private String buildArtworkSpec(Artwork artwork) {
        if (artwork == null) {
            return null;
        }
        return Arrays.asList(artwork.getMedium(), artwork.getSize(),
                        artwork.getYear() == null ? null : String.valueOf(artwork.getYear()))
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" / "));
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
