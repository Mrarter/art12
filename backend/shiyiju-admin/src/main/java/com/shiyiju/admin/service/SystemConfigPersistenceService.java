package com.shiyiju.admin.service;

import com.shiyiju.admin.service.support.SchemaInspector;
import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SystemConfigPersistenceService {

    private final JdbcTemplate jdbcTemplate;
    private final SchemaInspector schemaInspector;

    private static final List<ConfigItem> CONFIG_ITEMS = List.of(
        item("trade", "trade.order.timeout", "number", "订单超时时间", "orderTimeout", 30),
        item("trade", "trade.refund.days", "number", "退款处理周期", "refundDays", 7),
        item("trade", "trade.allow.repeat.buy", "boolean", "允许重复购买", "allowRepeatBuy", false),
        item("trade", "trade.price.unit", "string", "价格单位", "priceUnit", "fen"),

        item("promotion", "promotion.direct.commission", "number", "一级经纪人分成", "directCommission", 5),
        item("promotion", "promotion.team.commission", "number", "二级经纪人分成", "teamCommission", 2),
        item("promotion", "promotion.settlement.type", "string", "经纪人分成结算时机", "settlementType", "after_pay"),
        item("promotion", "promotion.min.withdraw", "number", "最低提现金额", "minWithdraw", 0),
        item("promotion", "promotion.withdraw.fee", "number", "提现手续费", "withdrawFee", 0),
        item("promotion", "promotion.withdraw.days", "number", "提现周期", "withdrawDays", 3),
        item("promotion", "promotion.promoter.condition", "string", "成为经纪人条件", "promoterCondition", "free"),
        item("promotion", "promotion.purchase.threshold", "number", "累计消费门槛", "purchaseThreshold", 1000),

        item("platformCommission", "platform.commission.enabled", "boolean", "平台抽佣开关", "enabled", true),
        item("platformCommission", "platform.commission.primary.sale.rate", "number", "普通订单平台抽佣比例", "primarySaleRate", 10),
        item("platformCommission", "platform.commission.resale.platform.fee.rate", "number", "转售平台服务费比例", "resalePlatformFeeRate", 15),
        item("platformCommission", "platform.commission.resale.artist.income.rate", "number", "转售艺术家持续收益比例", "resaleArtistIncomeRate", 5),
        item("platformCommission", "platform.commission.min.fee", "number", "最低平台服务费", "minPlatformFee", 0),
        item("platformCommission", "platform.commission.wallet.uid", "string", "平台收款钱包UID", "platformWalletUid", ""),
        item("platformCommission", "platform.commission.settlement.type", "string", "平台抽佣结算时机", "settlementType", "after_pay"),

        item("coupon", "coupon.enabled", "boolean", "优惠券开关", "enabled", true),
        item("coupon", "coupon.stack.enabled", "boolean", "允许叠加使用", "stackEnabled", false),
        item("coupon", "coupon.cash.enabled", "boolean", "现金券开关", "cashCouponEnabled", true),
        item("coupon", "coupon.cash.default.amount", "number", "现金券默认面额", "cashDefaultAmount", 10),
        item("coupon", "coupon.cash.min.order.amount", "number", "现金券最低订单金额", "cashMinOrderAmount", 100),
        item("coupon", "coupon.cash.max.discount.amount", "number", "现金券最高抵扣金额", "cashMaxDiscountAmount", 100),
        item("coupon", "coupon.cash.valid.days", "number", "现金券有效期", "cashValidDays", 30),
        item("coupon", "coupon.cash.user.limit", "number", "现金券单用户领取上限", "cashUserLimit", 1),
        item("coupon", "coupon.artist.commission.enabled", "boolean", "艺术家经纪人分成抵用券开关", "artistCommissionCouponEnabled", true),
        item("coupon", "coupon.artist.commission.default.rate", "number", "经纪人分成抵用券默认抵扣比例", "artistCommissionDefaultRate", 5),
        item("coupon", "coupon.artist.commission.max.amount", "number", "经纪人分成抵用券最高抵扣金额", "artistCommissionMaxAmount", 500),
        item("coupon", "coupon.artist.commission.valid.days", "number", "经纪人分成抵用券有效期", "artistCommissionValidDays", 30),
        item("coupon", "coupon.artist.commission.scope", "string", "经纪人分成抵用券适用范围", "artistCommissionScope", "artist_primary_sale"),
        item("coupon", "coupon.artist.commission.user.limit", "number", "经纪人分成抵用券单用户领取上限", "artistCommissionUserLimit", 1),

        item("trafficGrowth", "traffic.growth.page.view.enabled", "boolean", "全站页面浏览量增长开关", "pageViewGrowthEnabled", false),
        item("trafficGrowth", "traffic.growth.page.view.daily", "number", "全站页面每日浏览量增长", "pageDailyViewGrowth", 0),
        item("trafficGrowth", "traffic.growth.page.view.weekly", "number", "全站页面每周浏览量增长", "pageWeeklyViewGrowth", 0),
        item("trafficGrowth", "traffic.growth.page.view.monthly", "number", "全站页面每月浏览量增长", "pageMonthlyViewGrowth", 0),
        item("trafficGrowth", "traffic.growth.page.favorite.enabled", "boolean", "全站页面收藏量增长开关", "pageFavoriteGrowthEnabled", false),
        item("trafficGrowth", "traffic.growth.page.favorite.daily", "number", "全站页面每日收藏量增长", "pageDailyFavoriteGrowth", 0),
        item("trafficGrowth", "traffic.growth.page.favorite.weekly", "number", "全站页面每周收藏量增长", "pageWeeklyFavoriteGrowth", 0),
        item("trafficGrowth", "traffic.growth.page.favorite.monthly", "number", "全站页面每月收藏量增长", "pageMonthlyFavoriteGrowth", 0),
        item("trafficGrowth", "traffic.growth.artwork.enabled", "boolean", "作品日常热度增长开关", "artworkHeatGrowthEnabled", false),
        item("trafficGrowth", "traffic.growth.artwork.daily.view", "number", "作品每日浏览量增长", "artworkDailyViewGrowth", 0),
        item("trafficGrowth", "traffic.growth.artwork.daily.like", "number", "作品每日点赞量增长", "artworkDailyLikeGrowth", 0),
        item("trafficGrowth", "traffic.growth.artwork.daily.favorite", "number", "作品每日收藏量增长", "artworkDailyFavoriteGrowth", 0),
        item("trafficGrowth", "traffic.growth.artist.enabled", "boolean", "艺术家日常热度增长开关", "artistHeatGrowthEnabled", false),
        item("trafficGrowth", "traffic.growth.artist.daily.follow", "number", "艺术家每日关注量增长", "artistDailyFollowGrowth", 0),
        item("trafficGrowth", "traffic.growth.artist.daily.like", "number", "艺术家每日点赞量增长", "artistDailyLikeGrowth", 0),

        item("priceGrowth", "price.growth.enabled", "boolean", "价格增长开关", "enabled", true),
        item("priceGrowth", "price.growth.base.daily.rate", "number", "基础日增长率", "baseDailyRate", 0.0002),
        item("priceGrowth", "price.growth.mature.daily.rate", "number", "成熟期日增长率", "matureDailyRate", 0.0003),
        item("priceGrowth", "price.growth.mature.days", "number", "成熟期天数阈值", "matureDays", 30),
        item("priceGrowth", "price.growth.badge.default.rate", "number", "普通艺术家系数", "defaultBadgeRate", 1.0),
        item("priceGrowth", "price.growth.badge.verified.rate", "number", "认证艺术家系数", "verifiedBadgeRate", 1.5),
        item("priceGrowth", "price.growth.badge.popular.rate", "number", "人气艺术家系数", "popularBadgeRate", 2.0),
        item("priceGrowth", "price.growth.badge.master.rate", "number", "大师级艺术家系数", "masterBadgeRate", 3.0),
        item("priceGrowth", "price.growth.view.threshold", "number", "浏览量阈值", "viewThreshold", 100),
        item("priceGrowth", "price.growth.view.rate", "number", "浏览量加成", "viewRate", 1.1),
        item("priceGrowth", "price.growth.view.auto.enabled", "boolean", "浏览量自动增长开关", "viewAutoGrowthEnabled", false),
        item("priceGrowth", "price.growth.view.random.rate", "number", "浏览量自动增长随机浮动比例", "viewGrowthRandomRate", 0.58),
        item("priceGrowth", "price.growth.view.daily.growth", "number", "每日浏览量增长", "dailyViewGrowth", 0),
        item("priceGrowth", "price.growth.view.weekly.growth", "number", "每周浏览量增长", "weeklyViewGrowth", 0),
        item("priceGrowth", "price.growth.view.monthly.growth", "number", "每月浏览量增长", "monthlyViewGrowth", 0),
        item("priceGrowth", "price.growth.favorite.threshold", "number", "收藏量阈值", "favoriteThreshold", 5),
        item("priceGrowth", "price.growth.favorite.rate", "number", "收藏量加成", "favoriteRate", 1.1),
        item("priceGrowth", "price.growth.sale.rate", "number", "单次销售加成", "saleRate", 0.05),
        item("priceGrowth", "price.growth.max.sale.count", "number", "最多计算销售次数", "maxSaleCount", 10),
        item("priceGrowth", "price.growth.max.multiple", "number", "最大涨幅倍数", "maxGrowthMultiple", 5.0),

        item("auction", "auction.deposit.amount", "number", "拍卖保证金", "auctionDeposit", 1000),
        item("auction", "auction.deposit.refund", "boolean", "保证金退还", "depositRefund", true),
        item("auction", "auction.bid.increment", "number", "延时加价幅度", "bidIncrement", 100),
        item("auction", "auction.delay.cycles", "number", "延时周期", "delayCycles", 3),
        item("auction", "auction.delay.minutes", "number", "延时时长", "delayMinutes", 5),

        item("audit", "audit.artist.enabled", "boolean", "艺术家认证审核", "artistAudit", true),
        item("audit", "audit.artwork.enabled", "boolean", "作品审核", "artworkAudit", true),
        item("audit", "audit.post.enabled", "boolean", "动态审核", "postAudit", false),
        item("audit", "audit.sensitive.enabled", "boolean", "敏感词过滤", "sensitiveFilter", true),
        item("audit", "audit.sensitive.words", "text", "敏感词库", "sensitiveWords", "***\n***\n***")
    );

    public SystemConfigPersistenceService(JdbcTemplate jdbcTemplate, SchemaInspector schemaInspector) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaInspector = schemaInspector;
    }

    public Map<String, Object> getAllConfig() {
        String tableName = resolveConfigTable();
        Map<String, String> storedValues = new LinkedHashMap<>();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT config_key, config_value FROM " + tableName
        );
        for (Map<String, Object> row : rows) {
            storedValues.put(Objects.toString(row.get("config_key"), ""), Objects.toString(row.get("config_value"), ""));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (ConfigItem item : CONFIG_ITEMS) {
            @SuppressWarnings("unchecked")
            Map<String, Object> group = (Map<String, Object>) result.computeIfAbsent(item.group, key -> new LinkedHashMap<>());
            String raw = storedValues.get(item.configKey);
            group.put(item.field, raw == null ? item.defaultValue : parseValue(raw, item.configType, item.defaultValue));
        }
        return result;
    }

    public List<Map<String, Object>> getConfigGroups() {
        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(group("trade", "交易设置", "订单、退款与价格显示"));
        groups.add(group("promotion", "分销设置", "经纪人分成、提现和经纪人门槛"));
        groups.add(group("platformCommission", "平台抽佣", "普通订单、转售与平台钱包配置"));
        groups.add(group("coupon", "优惠券配置", "现金券和艺术家经纪人分成抵用券规则"));
        groups.add(group("trafficGrowth", "热度增长", "页面、作品、艺术家的浏览、点赞、收藏增长"));
        groups.add(group("priceGrowth", "价格增长", "价格增长系数与阈值"));
        groups.add(group("auction", "拍卖设置", "保证金与延时规则"));
        groups.add(group("audit", "审核设置", "审核开关与敏感词库"));
        return groups;
    }

    public List<Map<String, Object>> getConfigs(String groupName) {
        Map<String, Object> all = getAllConfig();
        List<Map<String, Object>> items = new ArrayList<>();
        for (ConfigItem item : CONFIG_ITEMS) {
            if (groupName != null && !groupName.isBlank() && !groupName.equals(item.group)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> group = (Map<String, Object>) all.get(item.group);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("configKey", item.configKey);
            row.put("configValue", group == null ? item.defaultValue : group.get(item.field));
            row.put("configType", item.configType);
            row.put("remark", item.remark);
            row.put("groupName", item.group);
            items.add(row);
        }
        return items;
    }

    public void updateAllConfig(Map<String, Object> payload) {
        String tableName = resolveConfigTable();
        boolean hasGroupName = schemaInspector.hasColumn(tableName, "group_name");
        boolean hasRemark = schemaInspector.hasColumn(tableName, "remark");
        boolean hasConfigName = schemaInspector.hasColumn(tableName, "config_name");
        boolean hasDescription = schemaInspector.hasColumn(tableName, "description");

        for (ConfigItem item : CONFIG_ITEMS) {
            Object groupValue = payload.get(item.group);
            if (!(groupValue instanceof Map<?, ?> groupMap) || !groupMap.containsKey(item.field)) {
                continue;
            }
            String rawValue = stringify(groupMap.get(item.field), item.defaultValue);
            if (hasGroupName || hasRemark) {
                jdbcTemplate.update(
                    """
                    INSERT INTO %s (config_key, config_value, config_type, group_name, remark)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        config_value = VALUES(config_value),
                        config_type = VALUES(config_type),
                        group_name = VALUES(group_name),
                        remark = VALUES(remark)
                    """.formatted(tableName),
                    item.configKey, rawValue, item.configType, item.group, item.remark
                );
            } else if (hasConfigName || hasDescription) {
                jdbcTemplate.update(
                    """
                    INSERT INTO %s (config_key, config_value, config_name, config_type, description)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        config_value = VALUES(config_value),
                        config_name = VALUES(config_name),
                        config_type = VALUES(config_type),
                        description = VALUES(description)
                    """.formatted(tableName),
                    item.configKey, rawValue, item.remark, item.configType, item.remark
                );
            }
        }
    }

    /**
     * 获取价格增长配置
     */
    public Map<String, Object> getPriceGrowthConfig() {
        Map<String, Object> all = getAllConfig();
        @SuppressWarnings("unchecked")
        Map<String, Object> priceGrowth = (Map<String, Object>) all.get("priceGrowth");
        return priceGrowth != null ? priceGrowth : new LinkedHashMap<>();
    }

    public Map<String, Object> getPlatformCommissionFinance() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("artistCertification", getArtistCertificationPaymentStats());
        result.put("afterPayCommission", getOrderCommissionStats("after_pay"));
        result.put("afterConfirmSettlement", getOrderCommissionStats("after_confirm"));
        result.put("afterRefundSettlement", getOrderCommissionStats("after_refund"));
        result.put("settlementType", readConfigValue("platform.commission.settlement.type", "after_pay"));
        result.put("refundDays", resolveConfigAmount("trade.refund.days", BigDecimal.valueOf(7)).intValue());
        result.put("updatedAt", LocalDateTime.now().toString());
        return result;
    }

    public PageResult<Map<String, Object>> getPlatformCommissionFlows(Integer page, Integer size, String keyword) {
        int safePage = Math.max(1, page == null ? 1 : page);
        int safeSize = Math.min(100, Math.max(1, size == null ? 10 : size));
        if (schemaInspector.getColumns("wallet_bill").isEmpty()) {
            return PageResult.empty(safePage, safeSize);
        }

        QueryParts query = platformCommissionFlowQuery(keyword);
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wallet_bill b " + query.joins() + query.where(),
            Long.class,
            query.args().toArray()
        );
        if (total == null || total <= 0) {
            return PageResult.empty(safePage, safeSize);
        }

        List<Object> args = new ArrayList<>(query.args());
        args.add((safePage - 1) * safeSize);
        args.add(safeSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            platformCommissionFlowSelect() + query.joins() + query.where() +
                " ORDER BY record_time DESC, b.id DESC LIMIT ?, ?",
            args.toArray()
        );
        return PageResult.of(total, safePage, safeSize, rows.stream().map(this::mapCommissionFlowRow).toList());
    }

    public Map<String, Object> getPlatformCommissionFlowDetail(Long billId) {
        if (billId == null || schemaInspector.getColumns("wallet_bill").isEmpty()) {
            return new LinkedHashMap<>();
        }
        QueryParts query = platformCommissionFlowQuery(null);
        List<Object> args = new ArrayList<>(query.args());
        args.add(billId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            platformCommissionFlowSelect() + query.joins() + query.where() + " AND b.id = ? LIMIT 1",
            args.toArray()
        );
        if (rows.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> detail = mapCommissionFlowRow(rows.get(0));
        detail.put("beforeBalance", normalizeMoney(decimalValue(rows.get(0).get("before_balance"))));
        detail.put("afterBalance", normalizeMoney(decimalValue(rows.get(0).get("after_balance"))));
        detail.put("goodsAmount", normalizeMoney(decimalValue(rows.get(0).get("goods_amount"))));
        detail.put("freightAmount", normalizeMoney(decimalValue(rows.get(0).get("freight_amount"))));
        detail.put("discountAmount", normalizeMoney(decimalValue(rows.get(0).get("discount_amount"))));
        detail.put("quantity", rows.get(0).get("quantity"));
        detail.put("orderId", rows.get(0).get("order_id"));
        detail.put("orderType", rows.get(0).get("order_type"));
        detail.put("orderStatus", rows.get(0).get("order_status"));
        detail.put("paymentStatus", rows.get(0).get("payment_status"));
        detail.put("paidAt", rows.get(0).get("paid_at"));
        detail.put("completedAt", rows.get(0).get("completed_at"));
        detail.put("remark", rows.get(0).get("remark"));
        detail.put("channelTradeNo", rows.get(0).get("channel_trade_no"));
        return detail;
    }

    /**
     * 更新价格增长配置
     */
    public void updatePriceGrowthConfig(Map<String, Object> params) {
        String tableName = resolveConfigTable();
        boolean hasGroupName = schemaInspector.hasColumn(tableName, "group_name");
        boolean hasRemark = schemaInspector.hasColumn(tableName, "remark");
        boolean hasConfigName = schemaInspector.hasColumn(tableName, "config_name");
        boolean hasDescription = schemaInspector.hasColumn(tableName, "description");

        for (ConfigItem item : CONFIG_ITEMS) {
            if (!"priceGrowth".equals(item.group)) {
                continue;
            }
            if (!params.containsKey(item.field)) {
                continue;
            }
            String rawValue = stringify(params.get(item.field), item.defaultValue);
            if (hasGroupName || hasRemark) {
                jdbcTemplate.update(
                    """
                    INSERT INTO %s (config_key, config_value, config_type, group_name, remark)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        config_value = VALUES(config_value),
                        config_type = VALUES(config_type),
                        group_name = VALUES(group_name),
                        remark = VALUES(remark)
                    """.formatted(tableName),
                    item.configKey, rawValue, item.configType, item.group, item.remark
                );
            } else if (hasConfigName || hasDescription) {
                jdbcTemplate.update(
                    """
                    INSERT INTO %s (config_key, config_value, config_name, config_type, description)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        config_value = VALUES(config_value),
                        config_name = VALUES(config_name),
                        config_type = VALUES(config_type),
                        description = VALUES(description)
                    """.formatted(tableName),
                    item.configKey, rawValue, item.remark, item.configType, item.remark
                );
            }
        }
    }

    private String resolveConfigTable() {
        String tableName = schemaInspector.resolveTable("config", "system_config", "sys_configs");
        if (!schemaInspector.getColumns(tableName).isEmpty()) {
            return tableName;
        }
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS system_config (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                config_key VARCHAR(100) NOT NULL,
                config_value TEXT,
                config_type VARCHAR(50) DEFAULT 'string',
                group_name VARCHAR(50) DEFAULT 'default',
                remark VARCHAR(255) DEFAULT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_config_key (config_key)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        return "system_config";
    }

    private Map<String, Object> getArtistCertificationPaymentStats() {
        BigDecimal amount = BigDecimal.ZERO;
        long count = 0L;

        if (!schemaInspector.getColumns("payment_order").isEmpty()) {
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS order_count, COALESCE(SUM(amount), 0) AS total_amount
                FROM payment_order
                WHERE status = 'SUCCESS'
                  AND UPPER(biz_type) IN (
                    'ARTIST_CERT', 'ARTIST_CERTIFICATION', 'ARTIST_VERIFY',
                    'ARTIST_AUTH', 'CERTIFICATION', 'REALNAME_CERT'
                  )
                """);
            count += longValue(row.get("order_count"));
            amount = amount.add(decimalValue(row.get("total_amount")));
        }

        if (!schemaInspector.getColumns("wallet_bill").isEmpty()) {
            String createdColumn = schemaInspector.firstExistingColumn("wallet_bill", "created_time", "create_time");
            String createdSelect = schemaInspector.hasColumn("wallet_bill", createdColumn) ? ", MAX(" + createdColumn + ") AS latest_time" : "";
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS bill_count, COALESCE(SUM(ABS(amount)), 0) AS total_amount %s
                FROM wallet_bill
                WHERE (LOWER(bill_type) IN ('artist_cert', 'artist_certification', 'certification', 'realname_cert')
                       OR remark LIKE '%%艺术家认证%%'
                       OR remark LIKE '%%认证支付%%'
                       OR remark LIKE '%%认证费用%%')
                """.formatted(createdSelect));
            count += longValue(row.get("bill_count"));
            amount = amount.add(decimalValue(row.get("total_amount")));
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("amount", normalizeMoney(amount));
        stats.put("count", count);
        stats.put("label", "艺术家认证支付费用");
        stats.put("description", "统计认证类支付订单与认证相关钱包流水");
        return stats;
    }

    private Map<String, Object> getOrderCommissionStats(String scope) {
        BigDecimal total = BigDecimal.ZERO;
        long count = 0L;
        if (!schemaInspector.getColumns("trade_order").isEmpty()) {
            List<Object> args = new ArrayList<>();
            StringBuilder where = new StringBuilder("""
                WHERE payment_status = 'PAID'
                  AND COALESCE(deleted, 0) = 0
                  AND UPPER(order_status) NOT IN ('CANCELLED', 'REFUNDED')
                """);
            if ("after_confirm".equals(scope)) {
                where.append(" AND UPPER(order_status) = 'COMPLETED'");
            } else if ("after_refund".equals(scope)) {
                int refundDays = resolveConfigAmount("trade.refund.days", BigDecimal.valueOf(7)).intValue();
                LocalDateTime cutoff = LocalDateTime.now().minusDays(Math.max(0, refundDays));
                where.append(" AND (paid_at <= ? OR completed_at <= ?)");
                args.add(cutoff);
                args.add(cutoff);
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, order_type, pay_amount FROM trade_order " + where,
                args.toArray()
            );
            for (Map<String, Object> row : rows) {
                BigDecimal commission = calculatePlatformCommission(
                    Objects.toString(row.get("order_type"), ""),
                    decimalValue(row.get("pay_amount"))
                );
                if (commission.compareTo(BigDecimal.ZERO) > 0) {
                    total = total.add(commission);
                    count++;
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("amount", normalizeMoney(total));
        stats.put("count", count);
        stats.put("label", switch (scope) {
            case "after_confirm" -> "确认收货后到账金额";
            case "after_refund" -> "超过退款期后到账金额";
            default -> "支付成功后平台抽佣营收";
        });
        stats.put("description", switch (scope) {
            case "after_confirm" -> "已完成订单按当前抽佣比例统计";
            case "after_refund" -> "已过退款周期订单按当前抽佣比例统计";
            default -> "已支付未退款订单按当前抽佣比例统计";
        });
        return stats;
    }

    private QueryParts platformCommissionFlowQuery(String keyword) {
        String userTable = resolveUserTable();
        String userUidColumn = schemaInspector.firstExistingColumn(userTable, "uid", "user_uid", "user_no");
        String userPhoneColumn = schemaInspector.firstExistingColumn(userTable, "phone", "mobile");
        String userNicknameColumn = schemaInspector.firstExistingColumn(userTable, "nickname", "name", "username");
        String userUidSelect = schemaInspector.hasColumn(userTable, userUidColumn) ? "bu." + userUidColumn : "CAST(bu.id AS CHAR)";
        String userPhoneSelect = schemaInspector.hasColumn(userTable, userPhoneColumn) ? "bu." + userPhoneColumn : "NULL";
        String userNicknameSelect = schemaInspector.hasColumn(userTable, userNicknameColumn) ? "bu." + userNicknameColumn : "NULL";

        StringBuilder joins = new StringBuilder();
        joins.append("""
             LEFT JOIN trade_order o ON b.related_type = 'order' AND b.related_id = o.id
             LEFT JOIN %s bu ON o.buyer_user_id = bu.id
            """.formatted(userTable));

        if (!schemaInspector.getColumns("trade_order_item").isEmpty()) {
            joins.append("""
                 LEFT JOIN (
                    SELECT order_id,
                           MIN(artist_id) AS seller_user_id,
                           MIN(artwork_id) AS artwork_id,
                           GROUP_CONCAT(DISTINCT item_title ORDER BY id SEPARATOR '、') AS artwork_title,
                           GROUP_CONCAT(DISTINCT cover_image ORDER BY id SEPARATOR ',') AS artwork_cover,
                           SUM(COALESCE(quantity, 0)) AS quantity
                    FROM trade_order_item
                    GROUP BY order_id
                 ) oi ON oi.order_id = o.id
                """);
        } else {
            joins.append("""
                 LEFT JOIN (
                    SELECT NULL AS order_id, NULL AS seller_user_id, NULL AS artwork_id, NULL AS artwork_title, NULL AS artwork_cover, NULL AS quantity
                 ) oi ON oi.order_id = o.id
                """);
        }
        joins.append(" LEFT JOIN %s su ON oi.seller_user_id = su.id\n".formatted(userTable));
        if (!schemaInspector.getColumns("payment_order").isEmpty()) {
            joins.append("""
                 LEFT JOIN (
                    SELECT biz_id,
                           MAX(channel) AS channel,
                           MAX(trade_type) AS trade_type,
                           MAX(channel_trade_no) AS channel_trade_no
                    FROM payment_order
                    WHERE UPPER(biz_type) IN ('ORDER', 'RESALE')
                    GROUP BY biz_id
                 ) p ON p.biz_id = o.id
                """);
        } else {
            joins.append("""
                 LEFT JOIN (
                    SELECT NULL AS biz_id, NULL AS channel, NULL AS trade_type, NULL AS channel_trade_no
                 ) p ON p.biz_id = o.id
                """);
        }

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
             WHERE (LOWER(b.bill_type) IN ('platform_fee', 'platform_commission', 'platform_service_fee')
                    OR b.remark LIKE '%平台抽佣%'
                    OR b.remark LIKE '%平台服务费%'
                    OR b.remark LIKE '%平台佣金%')
            """);
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            where.append("""
                 AND (o.order_no LIKE ?
                      OR oi.artwork_title LIKE ?
                      OR COALESCE(o.seller_name, %s) LIKE ?
                      OR %s LIKE ?
                      OR %s LIKE ?
                      OR %s LIKE ?)
                """.formatted(userNicknameSelect.replace("bu.", "su."), userNicknameSelect, userPhoneSelect, userUidSelect));
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        return new QueryParts(joins.toString(), where.toString(), args);
    }

    private String platformCommissionFlowSelect() {
        String billTime = timeColumnExpr("b", "wallet_bill", "created_time", "create_time", "created_at");
        String orderPaidTime = timeColumnExpr("o", "trade_order", "paid_at", "pay_time", "paid_time");
        String orderCreatedTime = timeColumnExpr("o", "trade_order", "created_at", "create_time", "created_time");
        String recordTime = "COALESCE(" + billTime + ", " + orderPaidTime + ", " + orderCreatedTime + ")";

        String userTable = resolveUserTable();
        String userUidColumn = schemaInspector.firstExistingColumn(userTable, "uid", "user_uid", "user_no");
        String userPhoneColumn = schemaInspector.firstExistingColumn(userTable, "phone", "mobile");
        String userNicknameColumn = schemaInspector.firstExistingColumn(userTable, "nickname", "name", "username");
        String userUidSelect = schemaInspector.hasColumn(userTable, userUidColumn) ? "bu." + userUidColumn : "CAST(bu.id AS CHAR)";
        String userPhoneSelect = schemaInspector.hasColumn(userTable, userPhoneColumn) ? "bu." + userPhoneColumn : "NULL";
        String userNicknameSelect = schemaInspector.hasColumn(userTable, userNicknameColumn) ? "bu." + userNicknameColumn : "NULL";
        String sellerUidSelect = userUidSelect.replace("bu.", "su.");
        String sellerPhoneSelect = userPhoneSelect.replace("bu.", "su.");
        String sellerNicknameSelect = userNicknameSelect.replace("bu.", "su.");

        return """
            SELECT b.id AS bill_id,
                   b.user_id AS platform_user_id,
                   b.bill_type,
                   b.amount,
                   b.before_balance,
                   b.after_balance,
                   b.related_id AS order_id,
                   b.remark,
                   %s AS record_time,
                   o.order_no,
                   o.order_type,
                   o.order_status,
                   o.payment_status,
                   o.pay_amount AS deal_amount,
                   o.goods_amount,
                   o.freight_amount,
                   o.discount_amount,
                   o.paid_at,
                   o.completed_at,
                   %s AS buyer_uid,
                   %s AS buyer_name,
                   %s AS buyer_phone,
                   %s AS seller_uid,
                   COALESCE(o.seller_name, %s) AS seller_name,
                   %s AS seller_phone,
                   oi.artwork_id,
                   oi.artwork_title,
                   SUBSTRING_INDEX(oi.artwork_cover, ',', 1) AS artwork_cover,
                   oi.quantity,
                   p.channel AS payment_channel,
                   p.trade_type AS client_type,
                   p.channel_trade_no
            FROM wallet_bill b
            """.formatted(
                recordTime,
                userUidSelect,
                userNicknameSelect,
                userPhoneSelect,
                sellerUidSelect,
                sellerNicknameSelect,
                sellerPhoneSelect
            );
    }

    private Map<String, Object> mapCommissionFlowRow(Map<String, Object> row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("billId", row.get("bill_id"));
        item.put("recordTime", row.get("record_time"));
        item.put("orderNo", row.get("order_no"));
        item.put("amount", normalizeMoney(decimalValue(row.get("amount"))));
        item.put("dealAmount", normalizeMoney(decimalValue(row.get("deal_amount"))));
        item.put("buyerUid", row.get("buyer_uid"));
        item.put("buyerName", row.get("buyer_name"));
        item.put("buyerPhone", row.get("buyer_phone"));
        item.put("sellerUid", row.get("seller_uid"));
        item.put("sellerName", row.get("seller_name"));
        item.put("sellerPhone", row.get("seller_phone"));
        item.put("artworkId", row.get("artwork_id"));
        item.put("artworkTitle", row.get("artwork_title"));
        item.put("artworkCover", row.get("artwork_cover"));
        item.put("clientType", row.get("client_type"));
        item.put("paymentChannel", row.get("payment_channel"));
        return item;
    }

    private String timeColumnExpr(String alias, String tableName, String... candidates) {
        String column = schemaInspector.firstExistingColumn(tableName, candidates);
        return schemaInspector.hasColumn(tableName, column) ? alias + "." + column : "NULL";
    }

    private String resolveUserTable() {
        if (!schemaInspector.getColumns("user_account").isEmpty()) {
            return "user_account";
        }
        return schemaInspector.resolveTable("system-config-user", "users", "sys_user");
    }

    private BigDecimal calculatePlatformCommission(String orderType, BigDecimal payAmount) {
        if (!isPlatformCommissionEnabled() || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = "RESALE".equalsIgnoreCase(orderType)
            ? resolveConfigAmount("platform.commission.resale.platform.fee.rate", BigDecimal.ZERO)
            : resolveConfigAmount("platform.commission.primary.sale.rate", BigDecimal.ZERO);
        BigDecimal fee = payAmount
            .multiply(rate)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal minFee = resolveConfigAmount("platform.commission.min.fee", BigDecimal.ZERO);
        if (fee.compareTo(BigDecimal.ZERO) > 0 && fee.compareTo(minFee) < 0) {
            fee = minFee;
        }
        return fee.min(payAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPlatformCommissionEnabled() {
        return Boolean.parseBoolean(readConfigValue("platform.commission.enabled", "true"));
    }

    private String readConfigValue(String key, String fallback) {
        try {
            String tableName = resolveConfigTable();
            String value = jdbcTemplate.queryForObject(
                "SELECT config_value FROM " + tableName + " WHERE config_key = ? LIMIT 1",
                String.class,
                key
            );
            return value == null || value.isBlank() ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private BigDecimal resolveConfigAmount(String key, BigDecimal fallback) {
        String value = readConfigValue(key, null);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
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
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? 0L : Long.parseLong(value.toString());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private Object parseValue(String raw, String configType, Object defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        try {
            return switch (configType) {
                case "boolean" -> Boolean.parseBoolean(raw);
                case "number" -> {
                    if (defaultValue instanceof Integer || defaultValue instanceof Long) {
                        yield raw.contains(".") ? (int) Double.parseDouble(raw) : Integer.parseInt(raw);
                    }
                    yield Double.parseDouble(raw);
                }
                default -> raw;
            };
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private String stringify(Object value, Object defaultValue) {
        Object target = value == null ? defaultValue : value;
        if (target instanceof Number || target instanceof Boolean) {
            return String.valueOf(target);
        }
        return Objects.toString(target, "");
    }

    private Map<String, Object> group(String code, String name, String description) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("code", code);
        group.put("name", name);
        group.put("description", description);
        return group;
    }

    private static ConfigItem item(String group, String configKey, String configType, String remark, String field, Object defaultValue) {
        return new ConfigItem(group, configKey, configType, remark, field, defaultValue);
    }

    private record ConfigItem(
        String group,
        String configKey,
        String configType,
        String remark,
        String field,
        Object defaultValue
    ) {}

    private record QueryParts(String joins, String where, List<Object> args) {}
}
