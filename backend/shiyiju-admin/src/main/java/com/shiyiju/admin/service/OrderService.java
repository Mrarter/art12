package com.shiyiju.admin.service;

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

    public OrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<Map<String, Object>> getOrders(String orderNo, String userName, Integer status,
                                                     String startDate, String endDate, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        List<Object> args = new ArrayList<>();
        
        if (orderNo != null && !orderNo.isEmpty()) {
            where.append(" AND order_no LIKE ?");
            args.add("%" + orderNo + "%");
        }
        if (status != null) {
            // 状态映射: 前端状态 -> 后端状态值
            String statusMapping = getStatusCondition(status);
            where.append(" AND ").append(statusMapping);
        }
        if (startDate != null) {
            where.append(" AND created_at >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            where.append(" AND created_at <= ?");
            args.add(endDate);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trade_order" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.id, o.order_no, o.buyer_user_id, o.order_type, o.order_status, o.payment_status,
                   o.goods_amount, o.freight_amount, o.discount_amount, o.pay_amount,
                   o.paid_at, o.cancelled_at, o.completed_at, o.created_at,
                   u.nickname as buyer_name, u.uid as buyer_uid
            FROM trade_order o
            LEFT JOIN user_account u ON o.buyer_user_id = u.id
            """ + where + " ORDER BY o.created_at DESC LIMIT ?, ?",
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
            item.put("status", row.get("order_status"));
            item.put("statusText", getOrderStatusText((String) row.get("order_status")));
            item.put("paymentStatus", row.get("payment_status"));
            item.put("totalAmount", row.get("goods_amount"));
            item.put("payAmount", row.get("pay_amount"));
            item.put("paidAt", row.get("paid_at"));
            item.put("createTime", row.get("created_at"));
            // 获取第一个商品信息
            Long orderId = ((Number) row.get("id")).longValue();
            List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT cover_url, item_title FROM trade_order_item WHERE order_id = ? LIMIT 1", orderId);
            if (!items.isEmpty()) {
                item.put("cover", items.get(0).get("cover_url"));
                item.put("artworkTitle", items.get(0).get("item_title"));
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
            "SELECT * FROM trade_order WHERE id = ? AND deleted = 0", id);
        if (rows.isEmpty()) return null;
        return convertOrder(rows.get(0));
    }
    
    public Map<String, Object> getOrderByNo(String orderNo) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.*, u.nickname as buyer_nickname, u.uid as buyer_uid
            FROM trade_order o
            LEFT JOIN user_account u ON o.buyer_user_id = u.id
            WHERE o.order_no = ? AND o.deleted = 0
            """, orderNo);
        if (rows.isEmpty()) return null;
        Map<String, Object> order = convertOrder(rows.get(0));
        // 添加买家信息
        order.put("buyerNickname", rows.get(0).get("buyer_nickname"));
        order.put("buyerUid", rows.get(0).get("buyer_uid"));
        // 获取订单商品信息
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            """
            SELECT toi.item_title, toi.cover_url, toi.unit_price, toi.quantity, toi.subtotal_amount,
                   a.title as artwork_title, a.cover_image 
            FROM trade_order_item toi
            LEFT JOIN artwork a ON toi.artwork_id = a.id
            WHERE toi.order_id = ?
            """, order.get("id"));
        order.put("items", items);
        return order;
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

    private String getStatusCondition(Integer status) {
        // 状态映射
        return switch (status) {
            case 1 -> "order_status = 'PENDING_PAYMENT'"; // 待付款
            case 2 -> "payment_status = 'PAID' AND order_status != 'COMPLETED'"; // 已付款/待发货
            case 3 -> "order_status = 'SHIPPED'"; // 已发货
            case 4 -> "order_status = 'COMPLETED'"; // 已完成
            case 5 -> "order_status = 'CANCELLED'"; // 已取消
            case 6 -> "order_status = 'REFUNDING' OR order_status = 'REFUNDED'"; // 退款
            default -> "1=1";
        };
    }

    private String getOrderStatusText(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "PENDING_PAYMENT" -> "待付款";
            case "PAID" -> "已付款";
            case "SHIPPED" -> "已发货";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "REFUNDING" -> "退款中";
            case "REFUNDED" -> "已退款";
            default -> status;
        };
    }

    /**
     * 售后/退款列表
     */
    public PageResult<Map<String, Object>> getRefunds(Integer status, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE o.order_status IN ('REFUNDING', 'REFUNDED')");
        List<Object> args = new ArrayList<>();

        if (status != null) {
            String statusCondition = status == 1 ? "o.order_status = 'REFUNDING'" : "o.order_status = 'REFUNDED'";
            where.append(" AND ").append(statusCondition);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trade_order o" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT o.id, o.order_no, o.buyer_user_id, o.order_type, o.order_status, o.payment_status,
                   o.goods_amount, o.pay_amount, o.paid_at, o.created_at,
                   u.nickname AS buyer_name, u.uid AS buyer_uid
            FROM trade_order o
            LEFT JOIN user_account u ON o.buyer_user_id = u.id
            """ + where + " ORDER BY o.created_at DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("orderNo", row.get("order_no"));
            item.put("userId", row.get("buyer_user_id"));
            item.put("orderType", row.get("order_type"));
            item.put("status", row.get("order_status"));
            item.put("statusText", getOrderStatusText((String) row.get("order_status")));
            item.put("payAmount", row.get("pay_amount"));
            item.put("amount", row.get("pay_amount"));  // 兼容前端字段名
            item.put("applyTime", row.get("paid_at"));
            item.put("createTime", row.get("created_at"));
            // 买家信息
            item.put("buyerName", row.get("buyer_name"));
            item.put("buyerUid", row.get("buyer_uid"));
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
        String newStatus;
        String paymentStatus = null;
        
        if (status == 1) {
            // 通过退款申请
            newStatus = "REFUNDED";
            paymentStatus = "REFUNDED";
        } else {
            // 拒绝退款申请，恢复原订单状态
            newStatus = "PAID";
            paymentStatus = "PAID";
        }
        
        // 更新订单状态
        jdbcTemplate.update(
            "UPDATE trade_order SET order_status = ?, payment_status = ?, update_time = ? WHERE id = ?",
            newStatus, paymentStatus, LocalDateTime.now(), id);
    }
}
