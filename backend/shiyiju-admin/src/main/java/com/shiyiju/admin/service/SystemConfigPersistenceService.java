package com.shiyiju.admin.service;

import com.shiyiju.admin.service.support.SchemaInspector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

        item("promotion", "promotion.direct.commission", "number", "一级佣金", "directCommission", 5),
        item("promotion", "promotion.team.commission", "number", "二级佣金", "teamCommission", 2),
        item("promotion", "promotion.settlement.type", "string", "佣金结算时机", "settlementType", "after_pay"),
        item("promotion", "promotion.min.withdraw", "number", "最低提现金额", "minWithdraw", 100),
        item("promotion", "promotion.withdraw.fee", "number", "提现手续费", "withdrawFee", 0),
        item("promotion", "promotion.withdraw.days", "number", "提现周期", "withdrawDays", 3),
        item("promotion", "promotion.promoter.condition", "string", "成为艺荐官条件", "promoterCondition", "free"),
        item("promotion", "promotion.purchase.threshold", "number", "累计消费门槛", "purchaseThreshold", 1000),

        item("priceGrowth", "price.growth.enabled", "boolean", "价格增长开关", "enabled", true),
        item("priceGrowth", "price.growth.base.daily.rate", "number", "基础日增长率", "baseDailyRate", 0.0002),
        item("priceGrowth", "price.growth.mature.daily.rate", "number", "成熟期日增长率", "matureDailyRate", 0.0003),
        item("priceGrowth", "price.growth.mature.days", "number", "成熟期天数阈值", "matureDays", 30),
        item("priceGrowth", "price.growth.badge.default.rate", "number", "普通艺术家系数", "defaultBadgeRate", 1.0),
        item("priceGrowth", "price.growth.badge.verified.rate", "number", "认证艺术家系数", "verifiedBadgeRate", 1.5),
        item("priceGrowth", "price.growth.badge.popular.rate", "number", "人气艺术家系数", "popularBadgeRate", 2.0),
        item("priceGrowth", "price.growth.badge.master.rate", "number", "大师级艺术家系数", "masterBadgeRate", 3.0),
        item("priceGrowth", "price.growth.view.threshold.1", "number", "浏览量阈值1", "viewThreshold1", 100),
        item("priceGrowth", "price.growth.view.rate.1", "number", "浏览量加成1", "viewRate1", 1.1),
        item("priceGrowth", "price.growth.view.threshold.2", "number", "浏览量阈值2", "viewThreshold2", 500),
        item("priceGrowth", "price.growth.view.rate.2", "number", "浏览量加成2", "viewRate2", 1.2),
        item("priceGrowth", "price.growth.view.threshold.3", "number", "浏览量阈值3", "viewThreshold3", 1000),
        item("priceGrowth", "price.growth.view.rate.3", "number", "浏览量加成3", "viewRate3", 1.3),
        item("priceGrowth", "price.growth.view.threshold.4", "number", "浏览量阈值4", "viewThreshold4", 5000),
        item("priceGrowth", "price.growth.view.rate.4", "number", "浏览量加成4", "viewRate4", 1.5),
        item("priceGrowth", "price.growth.favorite.threshold.1", "number", "收藏量阈值1", "favoriteThreshold1", 5),
        item("priceGrowth", "price.growth.favorite.rate.1", "number", "收藏量加成1", "favoriteRate1", 1.1),
        item("priceGrowth", "price.growth.favorite.threshold.2", "number", "收藏量阈值2", "favoriteThreshold2", 20),
        item("priceGrowth", "price.growth.favorite.rate.2", "number", "收藏量加成2", "favoriteRate2", 1.2),
        item("priceGrowth", "price.growth.favorite.threshold.3", "number", "收藏量阈值3", "favoriteThreshold3", 50),
        item("priceGrowth", "price.growth.favorite.rate.3", "number", "收藏量加成3", "favoriteRate3", 1.3),
        item("priceGrowth", "price.growth.favorite.threshold.4", "number", "收藏量阈值4", "favoriteThreshold4", 100),
        item("priceGrowth", "price.growth.favorite.rate.4", "number", "收藏量加成4", "favoriteRate4", 1.5),
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
        groups.add(group("promotion", "分销设置", "佣金、提现和艺荐官门槛"));
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
}
