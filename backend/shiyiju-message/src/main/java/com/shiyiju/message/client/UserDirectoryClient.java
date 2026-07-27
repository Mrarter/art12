package com.shiyiju.message.client;

import com.shiyiju.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class UserDirectoryClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${shiyiju.user-service-url:http://shiyiju-user:8081}")
    private String userServiceUrl;

    public Result<Map<String, Object>> getUserInfo(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        return restTemplate.exchange(
                userServiceUrl + "/user/info",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
        ).getBody();
    }
}
