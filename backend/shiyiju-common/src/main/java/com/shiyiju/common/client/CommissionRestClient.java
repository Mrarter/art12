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
 * 佣金 REST 客户端 - 供订单系统在支付后调佣佣金结算
 */
@Slf4j
@Component
public class CommissionRestClient {

    @Value("${promotion.service-url:http://127.0.0.1:8085}")
    private String promotionServiceUrl;

    private final RestTemplate restTemplate;

    public CommissionRestClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 结算佣金（订单支付后调用）
     */
    public boolean settleCommission(Long orderId, String orderNo, BigDecimal amount,
                                     Long buyerId, Long promoterId, Long artworkId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("orderId", orderId);
            body.put("orderNo", orderNo);
            body.put("amount", amount);
            body.put("buyerId", buyerId);
            body.put("promoterId", promoterId);
            body.put("artworkId", artworkId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = promotionServiceUrl + "/promoter/commission/settle";

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map respBody = response.getBody();
            if (respBody != null && respBody.get("code") != null && (Integer) respBody.get("code") == 200) {
                log.info("佣金结算成功: orderId={}", orderId);
                return true;
            }
            log.warn("佣金结算失败: orderId={}, response={}", orderId, respBody);
            return false;
        } catch (Exception e) {
            log.error("佣金结算调用异常: orderId={}", orderId, e);
            return false;
        }
    }
}
