package com.shiyiju.admin.controller;

import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 系统配置控制器
 */
@RestController
@RequestMapping("/admin/config")
public class SystemConfigController {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigController.class);

    /**
     * 获取配置分组列表
     */
    @GetMapping("/groups")
    public Result<List<Map<String, Object>>> getConfigGroups() {
        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(createGroup("commission", "分销配置", "佣金比例、团队奖励等"));
        groups.add(createGroup("order", "订单配置", "支付、发货等"));
        groups.add(createGroup("artwork", "作品配置", "审核、佣金等"));
        groups.add(createGroup("withdraw", "提现配置", "提现规则、手续费等"));
        groups.add(createGroup("price", "价格配置", "动态调价因子"));
        groups.add(createGroup("auction", "拍卖配置", "保证金、加价幅度等"));
        return Result.success(groups);
    }

    private Map<String, Object> createGroup(String code, String name, String desc) {
        Map<String, Object> group = new HashMap<>();
        group.put("code", code);
        group.put("name", name);
        group.put("description", desc);
        return group;
    }

    /**
     * 获取配置列表（按分组）
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getConfigs(@RequestParam(required = false) String group) {
        List<Map<String, Object>> configs = new ArrayList<>();
        
        if (group == null || "commission".equals(group)) {
            configs.add(createConfig("commission.direct.rate", "0.05", "直接推广佣金比例", "number"));
            configs.add(createConfig("commission.level.1.direct", "0.10", "一级佣金-普通艺荐官", "number"));
            configs.add(createConfig("commission.level.2.direct", "0.15", "一级佣金-高级艺荐官", "number"));
            configs.add(createConfig("commission.lock.period", "30", "锁客有效期(天)", "number"));
        }
        if (group == null || "price".equals(group)) {
            configs.add(createConfig("price.growth.enabled", "true", "启用动态调价", "boolean"));
            configs.add(createConfig("price.growth.days", "90", "上架N天后价格上浮", "number"));
            configs.add(createConfig("price.growth.max.rate", "0.50", "单作品累计涨幅上限", "number"));
        }
        if (group == null || "withdraw".equals(group)) {
            configs.add(createConfig("withdraw.min.amount", "100", "最低提现金额(元)", "number"));
            configs.add(createConfig("withdraw.fee.rate", "0", "提现手续费率(%)", "number"));
        }
        return Result.success(configs);
    }

    private Map<String, Object> createConfig(String key, String value, String remark, String type) {
        Map<String, Object> config = new HashMap<>();
        config.put("configKey", key);
        config.put("configValue", value);
        config.put("remark", remark);
        config.put("configType", type);
        return config;
    }

    /**
     * 更新配置
     */
    @PostMapping("/update")
    public Result<Void> updateConfig(@RequestBody Map<String, Object> params) {
        log.info("更新配置: {}", params);
        return Result.success();
    }
}
