package com.shiyiju.admin.service;

import com.shiyiju.admin.service.support.SchemaInspector;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.service.AlipayService;
import com.shiyiju.common.service.WxPayService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 订单管理服务 - 真实持久化（使用 JdbcTemplate）
 */
@Service
public class OrderService {

    private final JdbcTemplate jdbcTemplate;
    private final SchemaInspector schemaInspector;
    private final WxPayService wxPayService;
    private final AlipayService alipayService;

    public OrderService(JdbcTemplate jdbcTemplate, SchemaInspector schemaInspector,
                        WxPayService wxPayService, AlipayService alipayService) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaInspector = schemaInspector;
        this.wxPayService = wxPayService;
        this.alipayService = alipayService;
    }

    public PageResult<Map<String, Object>> getOrders(String orderNo, String userName, String status,
                                                     String startDate, String endDate, int page, int size) {
        String userTable = orderUserTable();
        String userUidSelect = userUidExpression("u", userTable);
        String userNameSelect = userNameExpression("u", userTable);
        String createdAtColumn = requiredOrderColumn("created_at", "create_time");
        String paidAtSelect = qualifiedColumnOrNull("o", "trade_order", "paid_at", "pay_time");
        String cancelledAtSelect = qualifiedColumnOrNull("o", "trade_order", "cancelled_at", "cancel_time");
        String completedAtSelect = qualifiedColumnOrNull("o", "trade_order", "completed_at", "finish_time", "complete_time");
        StringBuilder where = new StringBuilder(" WHERE " + orderDeletedCondition("o"));
        List<Object> args = new ArrayList<>();
        
        if (orderNo != null && !orderNo.isEmpty()) {
            where.append(" AND o.order_no LIKE ?");
            args.add("%" + orderNo + "%");
        }
        if (userName != null && !userName.isEmpty()) {
            where.append(" AND (").append(userNameSelect).append(" LIKE ? OR ").append(userUidSelect).append(" LIKE ?)");
            String keyword = "%" + userName + "%";
            args.add(keyword);
            args.add(keyword);
        }
        if (status != null && !status.isBlank()) {
            // 状态映射: 前端状态 -> 后端状态值
            String statusMapping = getStatusCondition(status);
            where.append(" AND ").append(statusMapping);
        }
        if (startDate != null) {
            where.append(" AND o.").append(createdAtColumn).append(" >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            where.append(" AND o.").append(createdAtColumn).append(" <= ?");
            args.add(endDate);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trade_order o LEFT JOIN " + userTable + " u ON o.buyer_user_id = u." + userPrimaryKeyColumn(userTable) + where,
            Long.class,
            args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.id, o.order_no, o.buyer_user_id, o.order_type, o.order_status, o.payment_status,
                   o.goods_amount, o.freight_amount, o.discount_amount, o.pay_amount,
                   %s AS paid_at, %s AS cancelled_at, %s AS completed_at, o.%s AS created_at,
                   %s AS buyer_name, %s AS buyer_uid
            FROM trade_order o
            LEFT JOIN %s u ON o.buyer_user_id = u.%s
            """.formatted(
                paidAtSelect,
                cancelledAtSelect,
                completedAtSelect,
                createdAtColumn,
                userNameSelect,
                userUidSelect,
                userTable,
                userPrimaryKeyColumn(userTable)
            ) + where + " ORDER BY o." + createdAtColumn + " DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        String itemCoverColumn = schemaInspector.firstExistingColumn("trade_order_item", "cover_url", "cover_image");
        String itemSubtotalColumn = schemaInspector.firstExistingColumn("trade_order_item", "subtotal_amount", "subtotal");
        String itemUnitPriceColumn = schemaInspector.firstExistingColumn("trade_order_item", "unit_price", "price");
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("orderNo", row.get("order_no"));
            item.put("userId", row.get("buyer_user_id"));
            item.put("buyerName", row.get("buyer_name") != null ? row.get("buyer_name") : "用户" + row.get("buyer_user_id"));
            item.put("buyerUid", row.get("buyer_uid"));
            item.put("orderType", row.get("order_type"));
            item.put("status", normalizeOrderStatus((String) row.get("order_status"), (String) row.get("payment_status")));
            item.put("rawStatus", row.get("order_status"));
            item.put("statusText", getOrderStatusText((String) row.get("order_status")));
            item.put("paymentStatus", row.get("payment_status"));
            item.put("totalAmount", row.get("goods_amount"));
            item.put("amount", row.get("pay_amount"));
            item.put("freight", row.get("freight_amount"));
            item.put("payAmount", row.get("pay_amount"));
            item.put("paidAt", row.get("paid_at"));
            item.put("createTime", row.get("created_at"));
            item.put("buyerPhone", null);
            // 获取第一个商品信息
            Long orderId = ((Number) row.get("id")).longValue();
            List<Map<String, Object>> items = jdbcTemplate.queryForList(
                """
                SELECT %s AS cover,
                       COALESCE(NULLIF(toi.item_title, ''), a.title) AS item_title,
                       toi.artwork_id,
                       %s AS subtotal_amount,
                       %s AS unit_price
                FROM trade_order_item toi
                LEFT JOIN artwork a ON toi.artwork_id = a.id
                WHERE toi.order_id = ?
                LIMIT 1
                """.formatted(
                    itemCoverExpression("toi", "a", itemCoverColumn),
                    numericItemColumnExpression("toi", itemSubtotalColumn),
                    numericItemColumnExpression("toi", itemUnitPriceColumn)
                ),
                orderId
            );
            if (!items.isEmpty()) {
                item.put("cover", items.get(0).get("cover"));
                item.put("artworkTitle", items.get(0).get("item_title"));
                item.put("artworkId", items.get(0).get("artwork_id"));
                item.put("firstItemSubtotal", items.get(0).get("subtotal_amount"));
                item.put("firstItemPrice", items.get(0).get("unit_price"));
            }
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    public Map<String, Object> getOrderById(Long id) {
        String userTable = orderUserTable();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.id, o.order_no, o.buyer_user_id, o.order_type, o.order_status, o.payment_status,
                   o.goods_amount, o.freight_amount, o.discount_amount, o.pay_amount, o.address_id,
                   %s AS paid_at, %s AS cancelled_at, %s AS completed_at, o.%s AS created_at,
                   %s AS buyer_nickname, %s AS buyer_uid
            FROM trade_order o
            LEFT JOIN %s u ON o.buyer_user_id = u.%s
            WHERE o.id = ? AND %s
            """.formatted(
                qualifiedColumnOrNull("o", "trade_order", "paid_at", "pay_time"),
                qualifiedColumnOrNull("o", "trade_order", "cancelled_at", "cancel_time"),
                qualifiedColumnOrNull("o", "trade_order", "completed_at", "finish_time", "complete_time"),
                requiredOrderColumn("created_at", "create_time"),
                userNameExpression("u", userTable),
                userUidExpression("u", userTable),
                userTable,
                userPrimaryKeyColumn(userTable),
                orderDeletedCondition("o")
            ), id);
        if (rows.isEmpty()) return null;
        Map<String, Object> order = convertOrder(rows.get(0));
        order.put("status", normalizeOrderStatus((String) rows.get(0).get("order_status"), (String) rows.get(0).get("payment_status")));
        order.put("buyerNickname", rows.get(0).get("buyer_nickname"));
        order.put("buyerUid", rows.get(0).get("buyer_uid"));
        ReceiverInfo receiver = resolveReceiverInfo(rows.get(0));
        order.put("buyerName", receiver.name().isBlank() ? rows.get(0).get("buyer_nickname") : receiver.name());
        order.put("buyerPhone", receiver.phone());
        order.put("address", receiver.address());
        order.put("items", getOrderItems(id));
        order.put("products", getOrderItems(id));
        order.put("logistics", getOrderLogistics(id));
        order.put("financialSummary", buildFinancialSummary(rows.get(0), id, String.valueOf(rows.get(0).get("order_no"))));
        return order;
    }
    
    public Map<String, Object> getOrderByNo(String orderNo) {
        String userTable = orderUserTable();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.id, o.order_no, o.buyer_user_id, o.order_type, o.order_status, o.payment_status,
                   o.goods_amount, o.freight_amount, o.discount_amount, o.pay_amount, o.address_id,
                   %s AS paid_at, %s AS cancelled_at, %s AS completed_at, o.%s AS created_at,
                   %s AS buyer_nickname, %s AS buyer_uid
            FROM trade_order o
            LEFT JOIN %s u ON o.buyer_user_id = u.%s
            WHERE o.order_no = ? AND %s
            """.formatted(
                qualifiedColumnOrNull("o", "trade_order", "paid_at", "pay_time"),
                qualifiedColumnOrNull("o", "trade_order", "cancelled_at", "cancel_time"),
                qualifiedColumnOrNull("o", "trade_order", "completed_at", "finish_time", "complete_time"),
                requiredOrderColumn("created_at", "create_time"),
                userNameExpression("u", userTable),
                userUidExpression("u", userTable),
                userTable,
                userPrimaryKeyColumn(userTable),
                orderDeletedCondition("o")
            ), orderNo);
        if (rows.isEmpty()) return null;
        Map<String, Object> order = convertOrder(rows.get(0));
        order.put("status", normalizeOrderStatus((String) rows.get(0).get("order_status"), (String) rows.get(0).get("payment_status")));
        // 添加买家信息
        order.put("buyerNickname", rows.get(0).get("buyer_nickname"));
        order.put("buyerUid", rows.get(0).get("buyer_uid"));
        ReceiverInfo receiver = resolveReceiverInfo(rows.get(0));
        order.put("buyerName", receiver.name().isBlank() ? rows.get(0).get("buyer_nickname") : receiver.name());
        order.put("buyerPhone", receiver.phone());
        order.put("address", receiver.address());
        // 获取订单商品信息
        Long id = ((Number) order.get("id")).longValue();
        order.put("items", getOrderItems(id));
        order.put("products", getOrderItems(id));
        order.put("logistics", getOrderLogistics(id));
        order.put("financialSummary", buildFinancialSummary(rows.get(0), id, orderNo));
        return order;
    }

    private Map<String, Object> buildFinancialSummary(Map<String, Object> orderRow, Long orderId, String orderNo) {
        BigDecimal goodsAmount = decimalValue(orderRow.get("goods_amount"));
        BigDecimal freightAmount = decimalValue(orderRow.get("freight_amount"));
        BigDecimal discountAmount = decimalValue(orderRow.get("discount_amount"));
        BigDecimal payAmount = decimalValue(orderRow.get("pay_amount"));
        BigDecimal platformCommissionAmount = resolvePlatformCommissionAmount(orderRow, orderId);
        List<Map<String, Object>> commissionDetails = getCommissionDetails(orderId, orderNo);
        BigDecimal brokerCommissionAmount = commissionDetails.stream()
            .map(item -> decimalValue(item.get("amount")))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settlementAmount = payAmount
            .subtract(platformCommissionAmount)
            .subtract(brokerCommissionAmount);
        if (settlementAmount.compareTo(BigDecimal.ZERO) < 0) {
            settlementAmount = BigDecimal.ZERO;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("goodsAmount", goodsAmount);
        summary.put("freightAmount", freightAmount);
        summary.put("discountAmount", discountAmount);
        summary.put("payAmount", payAmount);
        summary.put("platformCommissionAmount", platformCommissionAmount);
        summary.put("brokerCommissionAmount", brokerCommissionAmount);
        summary.put("settlementAmount", settlementAmount);
        summary.put("commissionDetails", commissionDetails);
        return summary;
    }

    private BigDecimal resolvePlatformCommissionAmount(Map<String, Object> orderRow, Long orderId) {
        for (String column : List.of("platform_commission_amount", "platform_commission", "platform_fee", "service_fee")) {
            if (schemaInspector.hasColumn("trade_order", column)) {
                BigDecimal amount = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(" + column + ", 0) FROM trade_order WHERE id = ?",
                    BigDecimal.class,
                    orderId
                );
                return amount == null ? BigDecimal.ZERO : amount;
            }
        }
        if (schemaInspector.getColumns("wallet_bill").isEmpty()) {
            return calculateConfiguredPlatformCommission(orderRow);
        }
        try {
            BigDecimal amount = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(ABS(amount)), 0)
                FROM wallet_bill
                WHERE related_id = ?
                  AND (bill_type IN ('platform_fee', 'platform', 'service_fee')
                       OR remark LIKE '%平台服务费%'
                       OR remark LIKE '%平台抽佣%')
                """,
                BigDecimal.class,
                orderId
            );
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                return amount;
            }
        } catch (Exception ignored) {
        }
        return calculateConfiguredPlatformCommission(orderRow);
    }

    private BigDecimal calculateConfiguredPlatformCommission(Map<String, Object> orderRow) {
        String orderType = String.valueOf(orderRow.getOrDefault("order_type", ""));
        if ("RESALE".equalsIgnoreCase(orderType)) {
            return BigDecimal.ZERO;
        }
        if (!isPlatformCommissionEnabled()) {
            return BigDecimal.ZERO;
        }
        BigDecimal payAmount = decimalValue(orderRow.get("pay_amount"));
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = resolveConfigRate("platform.commission.primary.sale.rate", BigDecimal.ZERO);
        BigDecimal fee = payAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal minFee = resolveConfigAmount("platform.commission.min.fee", BigDecimal.ZERO);
        if (fee.compareTo(BigDecimal.ZERO) > 0 && fee.compareTo(minFee) < 0) {
            fee = minFee;
        }
        return fee.min(payAmount);
    }

    private boolean isPlatformCommissionEnabled() {
        String raw = readConfigValue("platform.commission.enabled");
        return raw == null || raw.isBlank() || Boolean.parseBoolean(raw.trim());
    }

    private BigDecimal resolveConfigRate(String key, BigDecimal fallbackRate) {
        BigDecimal percent = resolveConfigAmount(key, null);
        if (percent != null) {
            return percent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return fallbackRate != null ? fallbackRate : BigDecimal.ZERO;
    }

    private BigDecimal resolveConfigAmount(String key, BigDecimal fallback) {
        String raw = readConfigValue(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            return fallback;
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

    private List<Map<String, Object>> getCommissionDetails(Long orderId, String orderNo) {
        List<Map<String, Object>> records = new ArrayList<>();
        records.addAll(getCommissionRecordDetails(orderId, orderNo));
        records.addAll(getLegacyCommissionLogDetails(orderId, orderNo, "commission_log"));
        records.addAll(getLegacyCommissionLogDetails(orderId, orderNo, "commission_logs"));
        return records;
    }

    private List<Map<String, Object>> getCommissionRecordDetails(Long orderId, String orderNo) {
        String table = "commission_record";
        if (schemaInspector.getColumns(table).isEmpty()) {
            return List.of();
        }
        String userTable = orderUserTable();
        String userJoinKey = userPrimaryKeyColumn(userTable);
        String userUidSelect = userUidExpression("u", userTable);
        String userNameSelect = userNameExpression("u", userTable);
        String createdSelect = schemaInspector.hasColumn(table, "created_time") ? "cr.created_time" : "NULL";
        String updatedSelect = schemaInspector.hasColumn(table, "updated_time") ? "cr.updated_time" : "NULL";
        String condition = schemaInspector.hasColumn(table, "order_no")
            ? "WHERE (cr.order_id = ? OR cr.order_no = ?)"
            : "WHERE cr.order_id = ?";
        Object[] args = schemaInspector.hasColumn(table, "order_no")
            ? new Object[] {orderId, orderNo}
            : new Object[] {orderId};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT cr.id, cr.user_id, %s AS receiver_name, %s AS receiver_uid,
                   cr.commission_type AS type, cr.commission_level AS level,
                   cr.rate AS rate, cr.amount AS amount, cr.status AS status,
                   cr.remark AS remark, %s AS created_time, %s AS updated_time
            FROM commission_record cr
            LEFT JOIN %s u ON cr.user_id = u.%s
            %s
            ORDER BY cr.commission_level ASC, cr.id ASC
            """.formatted(
                userNameSelect,
                userUidSelect,
                createdSelect,
                updatedSelect,
                userTable,
                userJoinKey,
                condition
            ),
            args
        );
        return normalizeCommissionRows(rows);
    }

    private List<Map<String, Object>> getLegacyCommissionLogDetails(Long orderId, String orderNo, String table) {
        if (schemaInspector.getColumns(table).isEmpty()) {
            return List.of();
        }
        String userColumn = schemaInspector.firstExistingColumn(table, "user_id", "promoter_id");
        String amountColumn = schemaInspector.firstExistingColumn(table, "commission_amount", "amount");
        String rateColumn = schemaInspector.firstExistingColumn(table, "commission_rate", "rate");
        String typeColumn = schemaInspector.firstExistingColumn(table, "commission_type", "type");
        String levelColumn = schemaInspector.firstExistingColumn(table, "level", "commission_level");
        String statusColumn = schemaInspector.firstExistingColumn(table, "status");
        String remarkColumn = schemaInspector.firstExistingColumn(table, "remark");
        String createdColumn = schemaInspector.firstExistingColumn(table, "create_time", "created_time");
        String userTable = orderUserTable();
        String userJoinKey = userPrimaryKeyColumn(userTable);
        String condition = schemaInspector.hasColumn(table, "order_no")
            ? "WHERE (cl.order_id = ? OR cl.order_no = ?)"
            : "WHERE cl.order_id = ?";
        Object[] args = schemaInspector.hasColumn(table, "order_no")
            ? new Object[] {orderId, orderNo}
            : new Object[] {orderId};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT cl.id, cl.%s AS user_id, %s AS receiver_name, %s AS receiver_uid,
                   %s AS type, %s AS level, %s AS rate, cl.%s AS amount,
                   %s AS status, %s AS remark, %s AS created_time, NULL AS updated_time
            FROM %s cl
            LEFT JOIN %s u ON cl.%s = u.%s
            %s
            ORDER BY cl.id ASC
            """.formatted(
                userColumn,
                userNameExpression("u", userTable),
                userUidExpression("u", userTable),
                prefixedColumnOrNull(table, "cl", typeColumn),
                prefixedColumnOrNull(table, "cl", levelColumn),
                prefixedColumnOrNull(table, "cl", rateColumn),
                amountColumn,
                prefixedColumnOrNull(table, "cl", statusColumn),
                prefixedColumnOrNull(table, "cl", remarkColumn),
                prefixedColumnOrNull(table, "cl", createdColumn),
                table,
                userTable,
                userColumn,
                userJoinKey,
                condition
            ),
            args
        );
        return normalizeCommissionRows(rows);
    }

    private List<Map<String, Object>> normalizeCommissionRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("userId", row.get("user_id"));
            item.put("receiverName", row.get("receiver_name"));
            item.put("receiverUid", row.get("receiver_uid"));
            item.put("type", row.get("type"));
            item.put("level", row.get("level"));
            item.put("rate", row.get("rate"));
            item.put("amount", row.get("amount"));
            item.put("status", row.get("status"));
            item.put("remark", row.get("remark"));
            item.put("createdTime", row.get("created_time"));
            item.put("updatedTime", row.get("updated_time"));
            records.add(item);
        }
        return records;
    }

    private List<Map<String, Object>> getOrderItems(Long orderId) {
        String coverColumn = schemaInspector.firstExistingColumn("trade_order_item", "cover_url", "cover_image");
        String priceColumn = schemaInspector.firstExistingColumn("trade_order_item", "unit_price", "price");
        String subtotalColumn = schemaInspector.firstExistingColumn("trade_order_item", "subtotal_amount", "subtotal");
        String yearSelect = schemaInspector.hasColumn("artwork", "year") ? "a.year" :
            (schemaInspector.hasColumn("artwork", "create_year") ? "a.create_year" : "NULL");
        String categoryNameSelect = schemaInspector.hasColumn("artwork", "category_name") ? "a.category_name" : "NULL";
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            """
            SELECT COALESCE(NULLIF(toi.item_title, ''), a.title) AS item_title,
                   %s AS cover,
                   %s AS price,
                   toi.quantity,
                   %s AS subtotal,
                   a.title AS artwork_title,
                   a.author_name AS artist_name,
                   a.size,
                   %s AS artwork_year,
                   COALESCE(NULLIF(a.art_type, ''), %s) AS art_type,
                   a.price AS artwork_price,
                   a.original_price AS artwork_original_price,
                   a.cover_image,
                   a.cover
            FROM trade_order_item toi
            LEFT JOIN artwork a ON toi.artwork_id = a.id
            WHERE toi.order_id = ?
            """.formatted(
                itemCoverExpression("toi", "a", coverColumn),
                numericItemColumnExpression("toi", priceColumn),
                numericItemColumnExpression("toi", subtotalColumn),
                yearSelect,
                categoryNameSelect
            ),
            orderId
        );
        return items;
    }

    @Transactional
    public void shipOrder(Long id, String expressCompany, String expressNo) {
        if (expressCompany == null || expressCompany.isBlank()) {
            throw new IllegalArgumentException("请选择快递公司");
        }
        if (expressNo == null || expressNo.isBlank()) {
            throw new IllegalArgumentException("请输入快递单号");
        }
        ensureLogisticsTables();

        List<Map<String, Object>> orders = jdbcTemplate.queryForList(
            "SELECT id, order_status, payment_status, address_id FROM trade_order WHERE id = ? AND deleted = 0",
            id);
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("订单不存在");
        }
        Map<String, Object> order = orders.get(0);
        String orderStatus = String.valueOf(order.get("order_status"));
        if (!("PAID".equalsIgnoreCase(orderStatus) || "WAIT_DELIVER".equalsIgnoreCase(orderStatus)
                || "WAIT_SHIP".equalsIgnoreCase(orderStatus))) {
            throw new IllegalStateException("订单状态不允许发货");
        }

        ReceiverInfo receiver = resolveReceiverInfo(order);
        String companyCode = logisticsCompanyCode(expressCompany);
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
            "SELECT id FROM logistics WHERE order_id = ? ORDER BY create_time DESC LIMIT 1", id);
        Long logisticsId;
        if (existing.isEmpty()) {
            jdbcTemplate.update("""
                INSERT INTO logistics (
                  order_id, company_code, company_name, tracking_no, ship_time, status,
                  receiver_name, receiver_phone, receiver_address, create_time, update_time
                ) VALUES (?, ?, ?, ?, NOW(), 1, ?, ?, ?, NOW(), NOW())
                """, id, companyCode, expressCompany, expressNo.trim(),
                receiver.name(), receiver.phone(), receiver.address());
            logisticsId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            logisticsId = ((Number) existing.get(0).get("id")).longValue();
            jdbcTemplate.update("""
                UPDATE logistics
                SET company_code = ?, company_name = ?, tracking_no = ?, ship_time = COALESCE(ship_time, NOW()),
                    status = 1, receiver_name = ?, receiver_phone = ?, receiver_address = ?, update_time = NOW()
                WHERE id = ?
                """, companyCode, expressCompany, expressNo.trim(),
                receiver.name(), receiver.phone(), receiver.address(), logisticsId);
        }

        jdbcTemplate.update("""
            INSERT INTO logistics_track (logistics_id, tracking_no, track_time, status, description, create_time)
            VALUES (?, ?, NOW(), '已发货', '包裹已发出，等待快递员取件', NOW())
            """, logisticsId, expressNo.trim());

        String updateColumn = schemaInspector.firstExistingColumn("trade_order", "updated_at", "update_time");
        String updateAssignment = updateColumn != null && schemaInspector.hasColumn("trade_order", updateColumn)
            ? ", " + updateColumn + " = NOW()"
            : "";
        jdbcTemplate.update(
            "UPDATE trade_order SET order_status = 'SHIPPED'" + updateAssignment + " WHERE id = ?",
            id);
    }

    private void ensureLogisticsTables() {
        if (schemaInspector.getColumns("logistics").isEmpty()) {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `logistics` (
                    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                    `order_id` BIGINT NOT NULL COMMENT '订单ID',
                    `company_code` VARCHAR(50) COMMENT '快递公司编码',
                    `company_name` VARCHAR(100) COMMENT '快递公司名称',
                    `tracking_no` VARCHAR(100) NOT NULL COMMENT '快递单号',
                    `ship_time` DATETIME COMMENT '发货时间',
                    `receive_time` DATETIME COMMENT '收货时间',
                    `status` TINYINT DEFAULT 1 COMMENT '物流状态: 1-已发货, 2-运输中, 3-派送中, 4-已签收',
                    `receiver_name` VARCHAR(100) COMMENT '收货人姓名',
                    `receiver_phone` VARCHAR(20) COMMENT '收货人电话',
                    `receiver_address` VARCHAR(500) COMMENT '收货地址',
                    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX `idx_order_id` (`order_id`),
                    INDEX `idx_tracking_no` (`tracking_no`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流信息表'
                """);
            schemaInspector.evictColumns("logistics");
        }
        if (schemaInspector.getColumns("logistics_track").isEmpty()) {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `logistics_track` (
                    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                    `logistics_id` BIGINT NOT NULL COMMENT '物流ID',
                    `tracking_no` VARCHAR(100) NOT NULL COMMENT '快递单号',
                    `track_time` DATETIME NOT NULL COMMENT '轨迹时间',
                    `status` VARCHAR(50) COMMENT '轨迹状态',
                    `description` VARCHAR(500) COMMENT '轨迹描述',
                    `location` VARCHAR(100) COMMENT '位置',
                    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX `idx_logistics_id` (`logistics_id`),
                    INDEX `idx_tracking_no` (`tracking_no`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹表'
                """);
            schemaInspector.evictColumns("logistics_track");
        }
    }

    private Map<String, Object> getOrderLogistics(Long orderId) {
        if (schemaInspector.getColumns("logistics").isEmpty()) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, company_code, company_name, tracking_no, ship_time, receive_time, status,
                   receiver_name, receiver_phone, receiver_address, create_time, update_time
            FROM logistics
            WHERE order_id = ?
            ORDER BY create_time DESC, id DESC
            LIMIT 1
            """, orderId);
        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> row = rows.get(0);
        int status = toInt(row.get("status"), 1);
        Map<String, Object> logistics = new LinkedHashMap<>();
        logistics.put("id", row.get("id"));
        logistics.put("companyCode", row.get("company_code"));
        logistics.put("companyName", row.get("company_name"));
        logistics.put("expressName", row.get("company_name"));
        logistics.put("trackingNo", row.get("tracking_no"));
        logistics.put("expressNo", row.get("tracking_no"));
        logistics.put("shipTime", row.get("ship_time"));
        logistics.put("receiveTime", row.get("receive_time"));
        logistics.put("status", status);
        logistics.put("statusText", logisticsStatusText(status));
        logistics.put("receiverName", row.get("receiver_name"));
        logistics.put("receiverPhone", row.get("receiver_phone"));
        logistics.put("receiverAddress", row.get("receiver_address"));
        logistics.put("traces", getOrderTracks(((Number) row.get("id")).longValue()));
        return logistics;
    }

    private List<Map<String, Object>> getOrderTracks(Long logisticsId) {
        if (schemaInspector.getColumns("logistics_track").isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT track_time, status, description, location
            FROM logistics_track
            WHERE logistics_id = ?
            ORDER BY track_time DESC, id DESC
            """, logisticsId);
        List<Map<String, Object>> tracks = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> track = new LinkedHashMap<>();
            track.put("time", row.get("track_time"));
            track.put("desc", row.get("description"));
            track.put("status", row.get("status"));
            track.put("location", row.get("location"));
            tracks.add(track);
        }
        return tracks;
    }

    private ReceiverInfo resolveReceiverInfo(Map<String, Object> order) {
        Object addressId = order.get("address_id");
        if (addressId == null || schemaInspector.getColumns("user_address").isEmpty()) {
            return new ReceiverInfo("", "", "");
        }

        String deletedCondition = schemaInspector.hasColumn("user_address", "deleted") ? " AND deleted = 0" : "";
        List<Map<String, Object>> addresses = jdbcTemplate.queryForList(
            """
            SELECT receiver_name, phone, province, city, district, detail_address
            FROM user_address
            WHERE id = ?
            """ + deletedCondition + " LIMIT 1",
            addressId);
        if (addresses.isEmpty()) {
            return new ReceiverInfo("", "", "");
        }
        Map<String, Object> address = addresses.get(0);
        String fullAddress = String.join("",
            nullToEmpty(address.get("province")),
            nullToEmpty(address.get("city")),
            nullToEmpty(address.get("district")),
            nullToEmpty(address.get("detail_address")));
        return new ReceiverInfo(
            nullToEmpty(address.get("receiver_name")),
            nullToEmpty(address.get("phone")),
            fullAddress);
    }

    private String logisticsCompanyCode(String companyName) {
        if (companyName == null) {
            return "";
        }
        if (companyName.contains("顺丰")) return "SF";
        if (companyName.contains("中通")) return "ZTO";
        if (companyName.contains("圆通")) return "YTO";
        if (companyName.contains("韵达")) return "YD";
        if (companyName.contains("申通")) return "STO";
        if (companyName.contains("邮政") || companyName.contains("EMS")) return "EMS";
        return companyName.trim();
    }

    private String logisticsStatusText(int status) {
        return switch (status) {
            case 2 -> "运输中";
            case 3 -> "派送中";
            case 4 -> "已签收";
            default -> "已发货";
        };
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private record ReceiverInfo(String name, String phone, String address) {}

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        jdbcTemplate.update(
            "UPDATE trade_order SET order_status = 'CANCELLED', cancel_reason = ? WHERE id = ?",
            reason, orderId);
    }

    public Map<String, Object> getOrderStats() {
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trade_order WHERE deleted = 0", Long.class);
        Long pendingPayment = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trade_order WHERE order_status = 'PENDING_PAYMENT' AND deleted = 0", Long.class);
        Long paid = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trade_order WHERE payment_status = 'PAID' AND deleted = 0", Long.class);
        BigDecimal totalAmount = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(pay_amount), 0) FROM trade_order WHERE payment_status = 'PAID' AND deleted = 0", BigDecimal.class);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOrders", total != null ? total : 0);
        stats.put("pendingPayment", pendingPayment != null ? pendingPayment : 0);
        stats.put("paid", paid != null ? paid : 0);
        stats.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        return stats;
    }

    private Map<String, Object> convertOrder(Map<String, Object> row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.get("id"));
        item.put("orderNo", row.get("order_no"));
        item.put("userId", row.get("buyer_user_id"));
        item.put("orderType", row.get("order_type"));
        item.put("orderStatus", row.get("order_status"));
        item.put("paymentStatus", row.get("payment_status"));
        item.put("goodsAmount", row.get("goods_amount"));
        item.put("freightAmount", row.get("freight_amount"));
        item.put("discountAmount", row.get("discount_amount"));
        item.put("payAmount", row.get("pay_amount"));
        item.put("paidAt", row.get("paid_at"));
        item.put("cancelledAt", row.get("cancelled_at"));
        item.put("completedAt", row.get("completed_at"));
        item.put("createTime", row.get("created_at") != null ? row.get("created_at") : row.get("create_time"));
        return item;
    }

    private String getStatusCondition(String status) {
        // 状态映射
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "1", "pending", "pending_payment" -> "o.order_status IN ('PENDING_PAYMENT', 'UNPAID')"; // 待付款
            case "2", "paid", "wait_deliver", "wait_ship" -> "o.payment_status = 'PAID' AND o.order_status IN ('PAID', 'WAIT_DELIVER', 'WAIT_SHIP')"; // 已付款/待发货
            case "3", "shipped" -> "o.order_status IN ('SHIPPED', 'DELIVERED')"; // 已发货
            case "4", "completed" -> "o.order_status = 'COMPLETED'"; // 已完成
            case "5", "cancelled", "canceled" -> "o.order_status IN ('CANCELLED', 'CANCELED')"; // 已取消
            case "6", "refund", "refunding", "refunded" -> "o.order_status IN ('REFUNDING', 'REFUNDED')"; // 退款
            default -> "1=1";
        };
    }

    private String normalizeOrderStatus(String orderStatus, String paymentStatus) {
        if (orderStatus == null) {
            return "unknown";
        }
        return switch (orderStatus) {
            case "PENDING_PAYMENT", "UNPAID" -> "pending";
            case "PAID", "WAIT_DELIVER", "WAIT_SHIP" -> "paid";
            case "SHIPPED", "DELIVERED" -> "shipped";
            case "COMPLETED" -> "completed";
            case "CANCELLED", "CANCELED" -> "cancelled";
            case "REFUNDING" -> "refunding";
            case "REFUNDED" -> "refunded";
            default -> "PAID".equals(paymentStatus) ? "paid" : orderStatus.toLowerCase(Locale.ROOT);
        };
    }

    private String getOrderStatusText(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "PENDING_PAYMENT", "UNPAID" -> "待付款";
            case "PAID", "WAIT_DELIVER", "WAIT_SHIP" -> "已付款";
            case "SHIPPED", "DELIVERED" -> "已发货";
            case "COMPLETED" -> "已完成";
            case "CANCELLED", "CANCELED" -> "已取消";
            case "REFUNDING" -> "退款中";
            case "REFUNDED" -> "已退款";
            default -> status;
        };
    }

    /**
     * 售后/退款列表
     */
    public PageResult<Map<String, Object>> getRefunds(String status, int page, int size) {
        if (schemaInspector.getColumns("refund_record").isEmpty()) {
            return getOrderRefunds(status, page, size);
        }

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            where.append(" AND rr.status = ?");
            args.add(mapRefundStatus(status));
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM refund_record rr LEFT JOIN trade_order o ON rr.order_id = o.id" + where,
            Long.class,
            args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT rr.id, rr.order_id, COALESCE(rr.order_no, o.order_no) AS order_no, rr.user_id,
                   rr.refund_amount, rr.refund_type, rr.reason, rr.status AS refund_status,
                   rr.handle_remark, rr.apply_time, rr.handle_time, rr.complete_time,
                   rr.return_company_name, rr.return_tracking_no, rr.return_status,
                   rr.return_ship_time, rr.return_receive_time,
                   o.order_type, o.order_status, o.payment_status, o.pay_amount,
                   u.nickname AS buyer_name, u.uid AS buyer_uid,
                   toi.cover_url, toi.item_title, toi.artwork_id
            FROM refund_record rr
            LEFT JOIN trade_order o ON rr.order_id = o.id
            LEFT JOIN user_account u ON rr.user_id = u.id
            LEFT JOIN (
                SELECT order_id, MIN(id) AS item_id
                FROM trade_order_item
                GROUP BY order_id
            ) first_item ON first_item.order_id = rr.order_id
            LEFT JOIN trade_order_item toi ON toi.id = first_item.item_id
            """ + where + " ORDER BY rr.apply_time DESC, rr.id DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            BigDecimal payAmount = decimalValue(row.get("pay_amount"));
            BigDecimal refundAmount = decimalValue(row.get("refund_amount"));
            BigDecimal actualPaidAmount = resolveActualPaidAmount(row);
            item.put("id", row.get("id"));
            item.put("orderId", row.get("order_id"));
            item.put("orderNo", row.get("order_no"));
            item.put("userId", row.get("user_id"));
            item.put("orderType", row.get("order_type"));
            item.put("status", normalizeRefundStatus(toInt(row.get("refund_status"), 0)));
            item.put("rawStatus", row.get("refund_status"));
            item.put("statusText", getOrderStatusText((String) row.get("order_status")));
            item.put("payAmount", payAmount);
            item.put("amount", refundAmount);
            item.put("actualPaidAmount", actualPaidAmount);
            item.put("displayAmount", actualPaidAmount != null && actualPaidAmount.compareTo(BigDecimal.ZERO) > 0
                ? actualPaidAmount
                : (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0 ? refundAmount : payAmount));
            item.put("refundType", row.get("refund_type"));
            item.put("type", toInt(row.get("refund_type"), 1) == 2 ? "return" : "refund");
            item.put("reason", row.get("reason"));
            item.put("remark", row.get("handle_remark"));
            item.put("applyTime", row.get("apply_time"));
            item.put("createTime", row.get("apply_time"));
            item.put("handleTime", row.get("handle_time"));
            item.put("completeTime", row.get("complete_time"));
            item.put("returnCompanyName", row.get("return_company_name"));
            item.put("returnTrackingNo", row.get("return_tracking_no"));
            item.put("returnStatus", row.get("return_status"));
            item.put("returnShipTime", row.get("return_ship_time"));
            item.put("returnReceiveTime", row.get("return_receive_time"));
            // 买家信息
            item.put("buyerName", row.get("buyer_name"));
            item.put("buyerUid", row.get("buyer_uid"));
            item.put("buyerPhone", null);
            item.put("cover", row.get("cover_url"));
            item.put("artworkTitle", row.get("item_title"));
            item.put("artworkId", row.get("artwork_id"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    private BigDecimal resolveActualPaidAmount(Map<String, Object> refundRow) {
        BigDecimal payAmount = decimalValue(refundRow.get("pay_amount"));
        if (schemaInspector.getColumns("payment_notify_log").isEmpty()) {
            return payAmount;
        }
        String orderNo = stringValue(refundRow.get("order_no"));
        if (orderNo == null || orderNo.isBlank()) {
            return payAmount;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT raw_payload
            FROM payment_notify_log
            WHERE channel = 'ALIPAY' AND biz_no = ?
            ORDER BY id DESC
            LIMIT 1
            """,
            orderNo
        );
        if (rows.isEmpty()) {
            return payAmount;
        }
        String payload = stringValue(rows.get(0).get("raw_payload"));
        BigDecimal amount = extractDecimal(payload, "receipt_amount");
        if (amount == null) {
            amount = extractDecimal(payload, "total_amount");
        }
        return amount != null ? amount : payAmount;
    }

    /**
     * 确认退货回寄已签收，并自动执行退款
     */
    @Transactional
    public void confirmRefundReturnReceived(Long id, String remark) {
        if (schemaInspector.getColumns("refund_record").isEmpty()) {
            throw new IllegalStateException("退款记录表不存在");
        }

        List<Map<String, Object>> refunds = jdbcTemplate.queryForList(
            """
            SELECT id, refund_type, status, return_tracking_no
            FROM refund_record
            WHERE id = ?
            LIMIT 1
            """,
            id);
        if (refunds.isEmpty()) {
            throw new IllegalStateException("退款记录不存在");
        }

        Map<String, Object> refund = refunds.get(0);
        int refundType = toInt(refund.get("refund_type"), 0);
        int refundStatus = toInt(refund.get("status"), 0);
        String trackingNo = stringValue(refund.get("return_tracking_no"));
        if (refundType != 2) {
            throw new IllegalStateException("当前售后类型不是退货退款");
        }
        if (refundStatus != 0) {
            throw new IllegalStateException("当前售后状态不允许确认签收");
        }
        if (trackingNo == null || trackingNo.isBlank()) {
            throw new IllegalStateException("买家尚未提交退货运单");
        }

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            """
            UPDATE refund_record
            SET return_status = 4, return_receive_time = ?, handle_remark = ?
            WHERE id = ?
            """,
            now,
            firstNonBlank(remark, "退货回寄已签收，系统自动退款"),
            id
        );

        handleRefund(id, 1, firstNonBlank(remark, "退货回寄已签收，系统自动退款"));
    }

    /**
     * 处理售后退款
     * @param id 订单ID
     * @param status 1=通过(已退款), 2=拒绝(恢复原状态)
     * @param remark 备注
     */
    @Transactional
    public void handleRefund(Long id, Integer status, String remark) {
        LocalDateTime now = LocalDateTime.now();
        if (schemaInspector.getColumns("refund_record").isEmpty()) {
            updateRefundOrderStatus(id, status, now);
            return;
        }
        List<Map<String, Object>> refunds = jdbcTemplate.queryForList(
            "SELECT id, order_id, refund_amount, reason FROM refund_record WHERE id = ? LIMIT 1", id);
        Long orderId = id;
        if (!refunds.isEmpty()) {
            orderId = ((Number) refunds.get(0).get("order_id")).longValue();
            if (status == 1) {
                initiateChannelRefund(orderId, refunds.get(0), remark, now);
            }
            jdbcTemplate.update(
                """
                UPDATE refund_record
                SET status = ?, handle_remark = ?, handle_time = ?, complete_time = ?
                WHERE id = ?
                """,
                status == 1 ? 1 : 2,
                remark,
                now,
                status == 1 ? now : null,
                id
            );
        }

        updateRefundOrderStatus(orderId, status, now);
    }

    private void initiateChannelRefund(Long orderId, Map<String, Object> refundRecord, String remark, LocalDateTime now) {
        if (schemaInspector.getColumns("refund_order").isEmpty() || schemaInspector.getColumns("payment_order").isEmpty()) {
            return;
        }
        Map<String, Object> refundOrder = findOrCreateRefundOrder(orderId, refundRecord, now);
        if (refundOrder == null) {
            throw new IllegalStateException("未找到可退款的支付单");
        }

        String refundNo = stringValue(refundOrder.get("refund_no"));
        String payNo = stringValue(refundOrder.get("pay_no"));
        String channel = stringValue(refundOrder.get("channel")).toUpperCase(Locale.ROOT);
        BigDecimal totalAmount = decimalValue(refundOrder.get("total_amount"));
        BigDecimal refundAmount = decimalValue(refundOrder.get("refund_amount"));
        String reason = firstNonBlank(stringValue(refundRecord.get("reason")), remark, "后台审核退款");

        jdbcTemplate.update(
            "UPDATE refund_order SET status = 'REFUNDING', request_payload = ?, update_time = ? WHERE refund_no = ?",
            "channel=" + channel + ", reason=" + reason,
            now,
            refundNo);

        try {
            Map<String, ?> result;
            if ("WECHAT".equals(channel)) {
                result = wxPayService.refundWithResult(
                    payNo,
                    refundNo,
                    String.valueOf(toPaymentFen(totalAmount)),
                    String.valueOf(toPaymentFen(refundAmount)),
                    reason);
                if (!"SUCCESS".equals(result.get("return_code")) || !"SUCCESS".equals(result.get("result_code"))) {
                    throw new IllegalStateException(firstNonBlank(
                        stringValue(result.get("err_code_des")),
                        stringValue(result.get("return_msg")),
                        "微信退款失败"));
                }
            } else if ("ALIPAY".equals(channel)) {
                BigDecimal actualPaidAmount = resolveAlipayPaidAmount(payNo, refundRecord);
                if (actualPaidAmount != null && actualPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
                    totalAmount = actualPaidAmount;
                    if (refundAmount == null || refundAmount.compareTo(actualPaidAmount) > 0) {
                        refundAmount = actualPaidAmount;
                    }
                }
                result = alipayService.refund(
                    payNo,
                    refundNo,
                    normalizeMoneyYuan(refundAmount),
                    reason);
            } else if ("UNKNOWN".equals(channel) || channel.isBlank()) {
                result = Map.of("manual", "true", "message", "无渠道支付单，按人工退款完成");
            } else {
                throw new IllegalStateException("暂不支持该退款渠道: " + channel);
            }

            Object channelRefundNo = result.containsKey("refund_id")
                ? result.get("refund_id")
                : (result.containsKey("tradeNo") ? result.get("tradeNo") : refundNo);
            jdbcTemplate.update(
                """
                UPDATE refund_order
                SET status = 'REFUNDED', channel_refund_no = ?, refund_time = ?, response_payload = ?, update_time = ?
                WHERE refund_no = ?
                """,
                stringValue(channelRefundNo),
                now,
                result.toString(),
                now,
                refundNo);
            jdbcTemplate.update("UPDATE payment_order SET status = 'REFUNDED', update_time = ? WHERE pay_no = ?",
                now, payNo);
        } catch (Exception e) {
            jdbcTemplate.update(
                """
                UPDATE refund_order
                SET status = 'FAILED', response_payload = ?, update_time = ?
                WHERE refund_no = ?
                """,
                e.getMessage(),
                now,
                refundNo);
            jdbcTemplate.update(
                "UPDATE refund_record SET handle_remark = ?, handle_time = ? WHERE id = ?",
                "渠道退款失败: " + e.getMessage(),
                now,
                refundRecord.get("id"));
            throw e;
        }
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

    private Map<String, Object> findOrCreateRefundOrder(Long orderId, Map<String, Object> refundRecord, LocalDateTime now) {
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
            """
            SELECT r.*, p.channel AS payment_channel, p.amount AS payment_amount
            FROM refund_order r
            LEFT JOIN payment_order p ON p.pay_no = r.pay_no
            WHERE r.biz_type = 'ORDER' AND r.biz_id = ?
            ORDER BY r.id DESC LIMIT 1
            """,
            orderId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        List<Map<String, Object>> payments = jdbcTemplate.queryForList(
            """
            SELECT p.*, o.order_no, o.buyer_user_id
            FROM payment_order p
            JOIN trade_order o ON o.id = p.biz_id
            WHERE p.biz_type = 'ORDER' AND p.biz_id = ? AND p.status IN ('SUCCESS', 'REFUNDING')
            ORDER BY p.id DESC LIMIT 1
            """,
            orderId);
        if (payments.isEmpty()) {
            return null;
        }

        Map<String, Object> payment = payments.get(0);
        String refundNo = "REF" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
        BigDecimal refundAmount = decimalValue(refundRecord.get("refund_amount"));
        jdbcTemplate.update(
            """
            INSERT INTO refund_order
                (refund_no, pay_no, biz_type, biz_id, biz_no, user_id, total_amount, refund_amount, channel, status, reason, create_time, update_time)
            VALUES (?, ?, 'ORDER', ?, ?, ?, ?, ?, ?, 'INIT', ?, ?, ?)
            """,
            refundNo,
            payment.get("pay_no"),
            orderId,
            payment.get("order_no"),
            payment.get("buyer_user_id"),
            payment.get("amount"),
            refundAmount,
            payment.get("channel"),
            refundRecord.get("reason"),
            now,
            now);

        return jdbcTemplate.queryForList("SELECT * FROM refund_order WHERE refund_no = ? LIMIT 1", refundNo).get(0);
    }

    private BigDecimal resolveAlipayPaidAmount(String payNo, Map<String, Object> refundRecord) {
        if (payNo == null || payNo.isBlank() || schemaInspector.getColumns("payment_notify_log").isEmpty()) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT raw_payload
            FROM payment_notify_log
            WHERE channel = 'ALIPAY' AND (pay_no = ? OR biz_no = ?)
            ORDER BY id DESC
            LIMIT 1
            """,
            payNo,
            stringValue(refundRecord.get("order_no"))
        );
        if (rows.isEmpty()) {
            return null;
        }
        String payload = stringValue(rows.get(0).get("raw_payload"));
        if (payload == null || payload.isBlank()) {
            return null;
        }
        BigDecimal amount = extractDecimal(payload, "receipt_amount");
        if (amount == null) {
            amount = extractDecimal(payload, "total_amount");
        }
        return amount;
    }

    private BigDecimal extractDecimal(String text, String key) {
        if (text == null || text.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([0-9]+(?:\\.[0-9]+)?)\"")
            .matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group(1));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void updateRefundOrderStatus(Long orderId, Integer status, LocalDateTime now) {
        String newStatus = status == 1 ? "REFUNDED" : "PAID";
        String paymentStatus = status == 1 ? "REFUNDED" : "PAID";
        String updateColumn = schemaInspector.firstExistingColumn("trade_order", "updated_at", "update_time");
        String updateAssignment = schemaInspector.hasColumn("trade_order", updateColumn)
            ? ", " + updateColumn + " = ?"
            : "";
        List<Object> args = new ArrayList<>();
        args.add(newStatus);
        args.add(paymentStatus);
        if (!updateAssignment.isEmpty()) {
            args.add(now);
        }
        args.add(orderId);
        jdbcTemplate.update(
            "UPDATE trade_order SET order_status = ?, payment_status = ?" + updateAssignment + " WHERE id = ?",
            args.toArray());
        syncUnifiedRefundStatus(orderId, status, now);
    }

    private void syncUnifiedRefundStatus(Long orderId, Integer status, LocalDateTime now) {
        if (schemaInspector.getColumns("refund_order").isEmpty()) {
            return;
        }
        String refundStatus = status == 1 ? "REFUNDED" : "CLOSED";
        jdbcTemplate.update(
            """
            UPDATE refund_order
            SET status = ?, refund_time = CASE WHEN ? = 'REFUNDED' THEN ? ELSE refund_time END, update_time = ?
            WHERE biz_type = 'ORDER' AND biz_id = ?
            """,
            refundStatus, refundStatus, now, now, orderId
        );
        if (!schemaInspector.getColumns("payment_order").isEmpty()) {
            String paymentStatus = status == 1 ? "REFUNDED" : "SUCCESS";
            jdbcTemplate.update(
                """
                UPDATE payment_order p
                JOIN refund_order r ON r.pay_no = p.pay_no
                SET p.status = ?, p.update_time = ?
                WHERE r.biz_type = 'ORDER' AND r.biz_id = ?
                """,
                paymentStatus, now, orderId
            );
        }
    }

    private PageResult<Map<String, Object>> getOrderRefunds(String status, int page, int size) {
        String userTable = orderUserTable();
        String createdAtColumn = requiredOrderColumn("created_at", "create_time");
        String paidAtSelect = qualifiedColumnOrNull("o", "trade_order", "paid_at", "pay_time");
        String itemCoverColumn = schemaInspector.firstExistingColumn("trade_order_item", "cover_url", "cover_image");
        StringBuilder where = new StringBuilder(" WHERE " + orderDeletedCondition("o"));
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            switch (status.trim().toLowerCase(Locale.ROOT)) {
                case "pending", "1", "refunding" -> where.append(" AND o.order_status = 'REFUNDING'");
                case "approved", "2", "refunded" -> where.append(" AND o.order_status = 'REFUNDED'");
                case "rejected", "3" -> where.append(" AND 1 = 0");
                default -> where.append(" AND o.order_status IN ('REFUNDING', 'REFUNDED')");
            }
        } else {
            where.append(" AND o.order_status IN ('REFUNDING', 'REFUNDED')");
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trade_order o" + where,
            Long.class,
            args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.id, o.order_no, o.buyer_user_id, o.order_type, o.order_status, o.payment_status,
                   o.pay_amount, %s AS paid_at, o.%s AS created_at,
                   %s AS buyer_name, %s AS buyer_uid,
                   toi.%s AS cover, toi.item_title, toi.artwork_id
            FROM trade_order o
            LEFT JOIN %s u ON o.buyer_user_id = u.%s
            LEFT JOIN (
                SELECT order_id, MIN(id) AS item_id
                FROM trade_order_item
                GROUP BY order_id
            ) first_item ON first_item.order_id = o.id
            LEFT JOIN trade_order_item toi ON toi.id = first_item.item_id
            """.formatted(
                paidAtSelect,
                createdAtColumn,
                userNameExpression("u", userTable),
                userUidExpression("u", userTable),
                itemCoverColumn,
                userTable,
                userPrimaryKeyColumn(userTable)
            ) + where + " ORDER BY o." + createdAtColumn + " DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("orderId", row.get("id"));
            item.put("orderNo", row.get("order_no"));
            item.put("userId", row.get("buyer_user_id"));
            item.put("orderType", row.get("order_type"));
            item.put("status", "REFUNDED".equals(row.get("order_status")) ? "approved" : "pending");
            item.put("rawStatus", row.get("order_status"));
            item.put("statusText", getOrderStatusText((String) row.get("order_status")));
            item.put("payAmount", row.get("pay_amount"));
            item.put("amount", row.get("pay_amount"));
            item.put("refundType", 1);
            item.put("reason", "订单售后申请");
            item.put("applyTime", row.get("paid_at"));
            item.put("createTime", row.get("created_at"));
            item.put("buyerName", row.get("buyer_name"));
            item.put("buyerUid", row.get("buyer_uid"));
            item.put("buyerPhone", null);
            item.put("cover", row.get("cover"));
            item.put("artworkTitle", row.get("item_title"));
            item.put("artworkId", row.get("artwork_id"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    public PageResult<Map<String, Object>> getPayments(String status, String channel, String keyword, int page, int size) {
        if (schemaInspector.getColumns("payment_order").isEmpty()) {
            return PageResult.empty(page, size);
        }
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            where.append(" AND p.status = ?");
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        if (channel != null && !channel.isBlank()) {
            where.append(" AND p.channel = ?");
            args.add(channel.trim().toUpperCase(Locale.ROOT));
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (p.pay_no LIKE ? OR p.biz_no LIKE ? OR p.channel_trade_no LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_order p" + where, Long.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
            """
            SELECT p.id, p.pay_no AS payNo, p.biz_type AS bizType, p.biz_id AS bizId, p.biz_no AS bizNo,
                   p.user_id AS userId, p.amount, p.channel, p.trade_type AS tradeType, p.status,
                   p.channel_trade_no AS channelTradeNo, p.expire_time AS expireTime, p.pay_time AS payTime,
                   p.create_time AS createTime, p.update_time AS updateTime,
                   o.order_status AS orderStatus, o.payment_status AS orderPaymentStatus
            FROM payment_order p
            LEFT JOIN trade_order o ON p.biz_type = 'ORDER' AND p.biz_id = o.id
            """ + where + " ORDER BY p.create_time DESC, p.id DESC LIMIT ?, ?",
            queryArgs.toArray());
        return PageResult.of(total == null ? 0L : total, page, size, records);
    }

    public PageResult<Map<String, Object>> getPaymentNotifyLogs(String channel, String keyword, int page, int size) {
        if (schemaInspector.getColumns("payment_notify_log").isEmpty()) {
            return PageResult.empty(page, size);
        }
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (channel != null && !channel.isBlank()) {
            where.append(" AND channel = ?");
            args.add(channel.trim().toUpperCase(Locale.ROOT));
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (pay_no LIKE ? OR biz_no LIKE ? OR channel_trade_no LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_notify_log" + where, Long.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
            """
            SELECT id, channel, pay_no AS payNo, biz_no AS bizNo, channel_trade_no AS channelTradeNo,
                   notify_type AS notifyType, verified, process_status AS processStatus,
                   fail_reason AS failReason, create_time AS createTime
            FROM payment_notify_log
            """ + where + " ORDER BY create_time DESC, id DESC LIMIT ?, ?",
            queryArgs.toArray());
        return PageResult.of(total == null ? 0L : total, page, size, records);
    }

    private Integer mapRefundStatus(String status) {
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "1", "pending", "refunding" -> 0;
            case "2", "approved", "refunded" -> 1;
            case "3", "rejected" -> 2;
            default -> null;
        };
    }

    private String orderUserTable() {
        if (schemaInspector.hasColumn("user_account", "identities")
            || schemaInspector.hasColumn("user_account", "uid")
            || schemaInspector.hasColumn("user_account", "user_uid")) {
            return "user_account";
        }
        if (schemaInspector.hasColumn("users", "uid")) {
            return "users";
        }
        if (schemaInspector.hasColumn("sys_user", "uid")) {
            return "sys_user";
        }
        return schemaInspector.resolveTable("order-user", "user_account", "users", "sys_user");
    }

    private String userPrimaryKeyColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "id", "user_id");
    }

    private String userNameExpression(String alias, String tableName) {
        for (String candidate : List.of("nickname", "name", "username", "real_name", "mobile", "phone")) {
            if (schemaInspector.hasColumn(tableName, candidate)) {
                return alias + "." + candidate;
            }
        }
        return "CAST(" + alias + "." + userPrimaryKeyColumn(tableName) + " AS CHAR)";
    }

    private String userUidExpression(String alias, String tableName) {
        List<String> columns = new ArrayList<>();
        for (String candidate : List.of("uid", "user_uid", "user_no")) {
            if (schemaInspector.hasColumn(tableName, candidate)) {
                columns.add(alias + "." + candidate);
            }
        }
        if (columns.isEmpty()) {
            return "CAST(" + alias + "." + userPrimaryKeyColumn(tableName) + " AS CHAR)";
        }
        columns.add("CAST(" + alias + "." + userPrimaryKeyColumn(tableName) + " AS CHAR)");
        return "COALESCE(" + String.join(", ", columns) + ")";
    }

    private String requiredOrderColumn(String... candidates) {
        for (String candidate : candidates) {
            if (schemaInspector.hasColumn("trade_order", candidate)) {
                return candidate;
            }
        }
        return candidates[0];
    }

    private String qualifiedColumnOrNull(String alias, String tableName, String... candidates) {
        for (String candidate : candidates) {
            if (schemaInspector.hasColumn(tableName, candidate)) {
                return alias + "." + candidate;
            }
        }
        return "NULL";
    }

    private String prefixedColumnOrNull(String tableName, String alias, String column) {
        if (column != null && !column.isBlank() && schemaInspector.hasColumn(tableName, column)) {
            return alias + "." + column;
        }
        return "NULL";
    }

    private String itemCoverExpression(String itemAlias, String artworkAlias, String itemCoverColumn) {
        StringBuilder expression = new StringBuilder();
        expression.append("COALESCE(NULLIF(")
            .append(itemAlias).append(".").append(itemCoverColumn)
            .append(", ''), ");
        String artworkCoverImage = qualifiedColumnOrNull(artworkAlias, "artwork", "cover_image");
        String artworkCover = qualifiedColumnOrNull(artworkAlias, "artwork", "cover");
        if (!"NULL".equals(artworkCoverImage)) {
            expression.append("NULLIF(").append(artworkCoverImage).append(", ''), ");
        }
        if (!"NULL".equals(artworkCover)) {
            expression.append("NULLIF(").append(artworkCover).append(", ''), ");
        }
        expression.append("NULL)");
        return expression.toString();
    }

    private String numericItemColumnExpression(String alias, String column) {
        if (column == null || column.isBlank()) {
            return "0";
        }
        return "COALESCE(" + alias + "." + column + ", 0)";
    }

    private String orderDeletedCondition(String alias) {
        if (schemaInspector.hasColumn("trade_order", "deleted")) {
            return alias + ".deleted = 0";
        }
        return "1 = 1";
    }

    private String normalizeRefundStatus(int status) {
        return switch (status) {
            case 1 -> "approved";
            case 2 -> "rejected";
            default -> "pending";
        };
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
