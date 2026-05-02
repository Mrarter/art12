package com.shiyiju.admin.service;

import com.shiyiju.admin.service.support.SchemaInspector;
import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 订单管理服务 - 真实持久化（使用 JdbcTemplate）
 */
@Service
public class OrderService {

    private final JdbcTemplate jdbcTemplate;
    private final SchemaInspector schemaInspector;

    public OrderService(JdbcTemplate jdbcTemplate, SchemaInspector schemaInspector) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaInspector = schemaInspector;
    }

    public PageResult<Map<String, Object>> getOrders(String orderNo, String userName, String status,
                                                     String startDate, String endDate, int page, int size) {
        String userUidSelect = userUidExpression("u");
        StringBuilder where = new StringBuilder(" WHERE o.deleted = 0");
        List<Object> args = new ArrayList<>();
        
        if (orderNo != null && !orderNo.isEmpty()) {
            where.append(" AND o.order_no LIKE ?");
            args.add("%" + orderNo + "%");
        }
        if (userName != null && !userName.isEmpty()) {
            where.append(" AND (u.nickname LIKE ? OR ").append(userUidSelect).append(" LIKE ?)");
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
            where.append(" AND o.created_at >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            where.append(" AND o.created_at <= ?");
            args.add(endDate);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trade_order o LEFT JOIN user_account u ON o.buyer_user_id = u.id" + where,
            Long.class,
            args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.id, o.order_no, o.buyer_user_id, o.order_type, o.order_status, o.payment_status,
                   o.goods_amount, o.freight_amount, o.discount_amount, o.pay_amount,
                   o.paid_at, o.cancelled_at, o.completed_at, o.created_at,
                   u.nickname as buyer_name, %s as buyer_uid
            FROM trade_order o
            LEFT JOIN user_account u ON o.buyer_user_id = u.id
            """.formatted(userUidSelect) + where + " ORDER BY o.created_at DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
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
                "SELECT cover_url, item_title, artwork_id FROM trade_order_item WHERE order_id = ? LIMIT 1", orderId);
            if (!items.isEmpty()) {
                item.put("cover", items.get(0).get("cover_url"));
                item.put("artworkTitle", items.get(0).get("item_title"));
                item.put("artworkId", items.get(0).get("artwork_id"));
            }
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    public Map<String, Object> getOrderById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.*, u.nickname AS buyer_nickname, %s AS buyer_uid
            FROM trade_order o
            LEFT JOIN user_account u ON o.buyer_user_id = u.id
            WHERE o.id = ? AND o.deleted = 0
            """.formatted(userUidExpression("u")), id);
        if (rows.isEmpty()) return null;
        Map<String, Object> order = convertOrder(rows.get(0));
        order.put("status", normalizeOrderStatus((String) rows.get(0).get("order_status"), (String) rows.get(0).get("payment_status")));
        order.put("buyerNickname", rows.get(0).get("buyer_nickname"));
        order.put("buyerUid", rows.get(0).get("buyer_uid"));
        order.put("items", getOrderItems(id));
        return order;
    }
    
    public Map<String, Object> getOrderByNo(String orderNo) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.*, u.nickname as buyer_nickname, %s as buyer_uid
            FROM trade_order o
            LEFT JOIN user_account u ON o.buyer_user_id = u.id
            WHERE o.order_no = ? AND o.deleted = 0
            """.formatted(userUidExpression("u")), orderNo);
        if (rows.isEmpty()) return null;
        Map<String, Object> order = convertOrder(rows.get(0));
        order.put("status", normalizeOrderStatus((String) rows.get(0).get("order_status"), (String) rows.get(0).get("payment_status")));
        // 添加买家信息
        order.put("buyerNickname", rows.get(0).get("buyer_nickname"));
        order.put("buyerUid", rows.get(0).get("buyer_uid"));
        // 获取订单商品信息
        order.put("items", getOrderItems(((Number) order.get("id")).longValue()));
        return order;
    }

    private List<Map<String, Object>> getOrderItems(Long orderId) {
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            """
            SELECT toi.item_title, toi.cover_url, toi.unit_price, toi.quantity, toi.subtotal_amount,
                   a.title as artwork_title, a.cover_image 
            FROM trade_order_item toi
            LEFT JOIN artwork a ON toi.artwork_id = a.id
            WHERE toi.order_id = ?
            """, orderId);
        return items;
    }

    @Transactional
    public void shipOrder(Long id, String expressCompany, String expressNo) {
        // 更新发货信息需要 shipment_order 表
        jdbcTemplate.update(
            "UPDATE trade_order SET order_status = 'SHIPPED' WHERE id = ?",
            id);
    }

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
        item.put("createTime", row.get("created_at"));
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
            item.put("id", row.get("id"));
            item.put("orderId", row.get("order_id"));
            item.put("orderNo", row.get("order_no"));
            item.put("userId", row.get("user_id"));
            item.put("orderType", row.get("order_type"));
            item.put("status", normalizeRefundStatus(toInt(row.get("refund_status"), 0)));
            item.put("rawStatus", row.get("refund_status"));
            item.put("statusText", getOrderStatusText((String) row.get("order_status")));
            item.put("payAmount", row.get("pay_amount"));
            item.put("amount", row.get("refund_amount"));
            item.put("refundType", row.get("refund_type"));
            item.put("reason", row.get("reason"));
            item.put("remark", row.get("handle_remark"));
            item.put("applyTime", row.get("apply_time"));
            item.put("createTime", row.get("apply_time"));
            item.put("handleTime", row.get("handle_time"));
            item.put("completeTime", row.get("complete_time"));
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
            "SELECT id, order_id FROM refund_record WHERE id = ? LIMIT 1", id);
        Long orderId = id;
        if (!refunds.isEmpty()) {
            orderId = ((Number) refunds.get(0).get("order_id")).longValue();
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
    }

    private PageResult<Map<String, Object>> getOrderRefunds(String status, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE o.deleted = 0");
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
                   o.pay_amount, o.paid_at, o.created_at,
                   u.nickname AS buyer_name, u.uid AS buyer_uid,
                   toi.cover_url, toi.item_title, toi.artwork_id
            FROM trade_order o
            LEFT JOIN user_account u ON o.buyer_user_id = u.id
            LEFT JOIN (
                SELECT order_id, MIN(id) AS item_id
                FROM trade_order_item
                GROUP BY order_id
            ) first_item ON first_item.order_id = o.id
            LEFT JOIN trade_order_item toi ON toi.id = first_item.item_id
            """ + where + " ORDER BY o.created_at DESC LIMIT ?, ?",
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

    private Integer mapRefundStatus(String status) {
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "1", "pending", "refunding" -> 0;
            case "2", "approved", "refunded" -> 1;
            case "3", "rejected" -> 2;
            default -> null;
        };
    }

    private String userUidExpression(String alias) {
        List<String> columns = new ArrayList<>();
        for (String candidate : List.of("uid", "user_uid", "user_no")) {
            if (schemaInspector.hasColumn("user_account", candidate)) {
                columns.add(alias + "." + candidate);
            }
        }
        if (columns.isEmpty()) {
            return "CAST(" + alias + ".id AS CHAR)";
        }
        columns.add("CAST(" + alias + ".id AS CHAR)");
        return "COALESCE(" + String.join(", ", columns) + ")";
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
}
