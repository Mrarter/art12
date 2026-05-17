package com.shiyiju.common.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 钱包 REST 客户端 - 用于其他服务通过 HTTP 调用钱包 API
 */
@Slf4j
@Component
public class WalletRestClient {

    @Value("${wallet.service-url:http://127.0.0.1:8081}")
    private String walletServiceUrl;

    @Value("${wallet.admin-key:shiyiju-wallet-admin-2026}")
    private String adminKey;

    private final RestTemplate restTemplate;

    public WalletRestClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 钱包入账
     */
    public boolean income(Long userId, BigDecimal amount, String billType,
                          Long relatedId, String relatedType, String remark) {
        return call("/admin/income", userId, amount, billType, relatedId, relatedType, remark);
    }

    /**
     * 钱包出账
     */
    public boolean expense(Long userId, BigDecimal amount, String billType,
                           Long relatedId, String relatedType, String remark) {
        return call("/admin/expense", userId, amount, billType, relatedId, relatedType, remark);
    }

    /**
     * 冻结
     */
    public boolean freeze(Long userId, BigDecimal amount,
                          Long relatedId, String relatedType, String remark) {
        return call("/admin/freeze", userId, amount, "freeze", relatedId, relatedType, remark);
    }

    /**
     * 解冻
     */
    public boolean unfreeze(Long userId, BigDecimal amount,
                            Long relatedId, String relatedType, String remark) {
        return call("/admin/unfreeze", userId, amount, "unfreeze", relatedId, relatedType, remark);
    }

    private boolean call(String path, Long userId, BigDecimal amount, String billType,
                         Long relatedId, String relatedType, String remark) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Wallet-Admin-Key", adminKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("amount", amount);
            body.put("billType", billType);
            body.put("relatedId", relatedId);
            body.put("relatedType", relatedType);
            body.put("remark", remark);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = walletServiceUrl + "/user/wallet" + path;

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map respBody = response.getBody();
            if (respBody != null && respBody.get("code") != null && (Integer) respBody.get("code") == 200) {
                log.info("钱包操作成功: {} userId={} amount={}", path, userId, amount);
                return true;
            }
            log.warn("钱包操作失败: {} userId={} amount={} response={}", path, userId, amount, respBody);
            return false;
        } catch (Exception e) {
            log.error("钱包调用异常: {} userId={} amount={}", path, userId, amount, e);
            return false;
        }
    }
}
