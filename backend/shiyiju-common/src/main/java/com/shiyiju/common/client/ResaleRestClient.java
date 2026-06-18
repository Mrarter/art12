package com.shiyiju.common.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 转售 REST 客户端 - 供订单系统在支付后调用转售标记已支付
 */
@Slf4j
@Component
public class ResaleRestClient {

    @Value("${resale.service-url:http://127.0.0.1:8081}")
    private String resaleServiceUrl;

    @Value("${wallet.admin-key:shiyiju-wallet-admin-2026}")
    private String adminKey;

    private final RestTemplate restTemplate;

    public ResaleRestClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 标记转售为已支付（订单支付回调后调用）
     */
    public boolean markAsPaid(Long resaleId, Long buyerUserId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Wallet-Admin-Key", adminKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("resaleId", resaleId);
            body.put("buyerUserId", buyerUserId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = resaleServiceUrl + "/admin/resale/mark-paid";

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map respBody = response.getBody();
            if (respBody != null && respBody.get("code") != null
                    && ((Number) respBody.get("code")).intValue() == 200) {
                log.info("转售标记已支付成功: resaleId={}, buyerUserId={}", resaleId, buyerUserId);
                return true;
            }
            log.warn("转售标记已支付失败: resaleId={}, buyerUserId={}, response={}", resaleId, buyerUserId, respBody);
            return false;
        } catch (Exception e) {
            log.error("转售标记已支付调用异常: resaleId={}, buyerUserId={}", resaleId, buyerUserId, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getDetail(Long resaleId) {
        if (resaleId == null) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Wallet-Admin-Key", adminKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = resaleServiceUrl + "/admin/resale/" + resaleId;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || body.get("code") == null || ((Number) body.get("code")).intValue() != 200) {
                log.warn("获取转售详情失败: resaleId={}, response={}", resaleId, body);
                return null;
            }
            Object data = body.get("data");
            return data instanceof Map ? (Map<String, Object>) data : null;
        } catch (Exception e) {
            log.error("获取转售详情调用异常: resaleId={}", resaleId, e);
            return null;
        }
    }
}
