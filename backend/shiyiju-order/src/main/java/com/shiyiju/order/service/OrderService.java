package com.shiyiju.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.constant.OrderConstant;
import com.shiyiju.common.constant.ProductConstant;
import com.shiyiju.common.client.CommissionRestClient;
import com.shiyiju.common.client.ResaleRestClient;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final WxPayService wxPayService;
    private final AlipayService alipayService;
    private final ResaleRestClient resaleRestClient;
    private final WalletRestClient walletClient;
    private final CommissionRestClient commissionRestClient;
    private final FinanceEventPublisher financeEventPublisher;
    private final OrderFailRecorder orderFailRecorder;
    private final LogisticsMapper logisticsMapper;
    private final LogisticsService logisticsService;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate productRestTemplate = new RestTemplate();

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    // 佣金比例
    private static final BigDecimal DIRECT_COMMISSION_RATE = new BigDecimal("0.05"); // 一级佣金 5%

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

                if (!hasEnoughStockForOrder(artwork, cart.getQuantity())) {
                    throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, "作品【" + artwork.getTitle() + "】库存不足");
                }

                OrderItem item = createOrderItem(artwork, cart.getQuantity(), null);
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
            if (!hasEnoughStockForOrder(artwork, qty)) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }

            OrderItem item = createOrderItem(artwork, qty, dto.getPromoterId());
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
        }

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

    private OrderItem createOrderItem(Artwork artwork, int quantity, Long promoterId) {
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
        item.setPromoterId(promoterId);
        return item;
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

    private BigDecimal resolveProductCurrentPrice(Artwork artwork) {
        if (artwork == null || artwork.getId() == null) {
            return null;
        }
        try {
            Map<?, ?> response = productRestTemplate.getForObject(
                    "http://127.0.0.1:8082/product/" + artwork.getId(),
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
        item.setArtistId(artwork.getAuthorId());
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
        List<OrderItem> sellerItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getArtistId, sellerUserId)
                        .orderByDesc(OrderItem::getCreateTime)
        );
        List<Long> orderIds = sellerItems.stream()
                .map(OrderItem::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return PageResult.of(0L, page, pageSize, Collections.emptyList());
        }

        LambdaQueryWrapper<Order> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.in(Order::getId, orderIds);
        if (status != null && !"all".equals(status)) {
            countWrapper.eq(Order::getStatus, status);
        }
        Long total = orderMapper.selectCount(countWrapper);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getId, orderIds);
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
        normalizeZeroAmountPendingOrders(userId);
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
        logisticsService.confirmReceive(orderId, userId);
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

    /** 支付宝手机网站支付下单。 */
    public Map<String, Object> createAlipayWapPay(Long orderId, Long userId) {
        Order order = getPayableOrder(orderId, userId);
        BigDecimal amountYuan = order.getPayAmount().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String description = getOrderDescription(orderId);

        try {
            Map<String, Object> payParams = new HashMap<>(alipayService.createWapPay(
                    order.getOrderNo(),
                    amountYuan,
                    description
            ));
            payParams.put("description", description);
            redisTemplate.opsForValue().set("pay:order:" + orderId, order.getOrderNo(), 30, TimeUnit.MINUTES);
            log.info("支付宝下单成功 - 订单ID: {}, OrderNo: {}", orderId, order.getOrderNo());
            return payParams;
        } catch (Exception e) {
            log.error("支付宝下单失败", e);
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付宝下单失败: " + e.getMessage());
        }
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
        if (!OrderConstant.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
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

        // === 普通订单：为每个订单项发布艺术家收益事件 ===
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        markOrderArtworksSold(order, items);
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
                walletClient.income(artwork.getAuthorId(), item.getPrice(), "order_sale",
                        order.getId(), "order",
                        "作品销售: " + artwork.getTitle() + " " + orderNo);
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

    /** 支付成功后才确认作品归属和已收藏状态。 */
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

            LambdaUpdateWrapper<Artwork> update = new LambdaUpdateWrapper<Artwork>()
                    .eq(Artwork::getId, artwork.getId())
                    .eq(Artwork::getStatus, ProductConstant.STATUS_ON_SALE)
                    .setSql("stock = GREATEST(COALESCE(stock, 1) - " + quantity + ", 0)")
                    .set(Artwork::getStatus, ProductConstant.STATUS_SOLD_OUT)
                    .set(Artwork::getHolderId, order.getUserId())
                    .set(Artwork::getHolderSince, LocalDateTime.now())
                    .setSql("sale_count = COALESCE(sale_count, 0) + " + quantity);

            if (quantity <= 1) {
                update.and(wrapper -> wrapper.isNull(Artwork::getStock)
                        .or().le(Artwork::getStock, 0)
                        .or().ge(Artwork::getStock, quantity));
            } else {
                update.and(wrapper -> wrapper.isNull(Artwork::getStock)
                        .or().ge(Artwork::getStock, quantity));
            }

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
            if (!OrderConstant.SOURCE_RESALE.equals(order.getSource())) {
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
            if (!OrderConstant.SOURCE_RESALE.equals(order.getSource())) {
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

        return vo;
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
