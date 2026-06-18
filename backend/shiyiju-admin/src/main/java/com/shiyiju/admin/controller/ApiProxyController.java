package com.shiyiju.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Enumeration;
import java.util.Set;

@RestController
public class ApiProxyController {

    private static final Set<String> SKIPPED_HEADERS = Set.of(
        "host",
        "connection",
        "transfer-encoding",
        "content-length"
    );

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${shiyiju.services.product-url:http://localhost:8082}")
    private String productServiceBaseUrl;

    @RequestMapping({
        "/api/product/**",
        "/api/artist/score/**",
        "/api/admin/artist/score/**",
        "/admin/artist/score/**"
    })
    public ResponseEntity<byte[]> proxyProductApi(
        HttpServletRequest request,
        @RequestBody(required = false) byte[] body
    ) {
        String targetPath = resolveProductTargetPath(request.getRequestURI());
        URI targetUri = UriComponentsBuilder
            .fromHttpUrl(serviceUrl(productServiceBaseUrl, targetPath))
            .query(request.getQueryString())
            .build(true)
            .toUri();

        ResponseEntity<byte[]> response = restTemplate.exchange(
            targetUri,
            HttpMethod.valueOf(request.getMethod()),
            new HttpEntity<>(body, copyHeaders(request)),
            byte[].class
        );

        HttpHeaders responseHeaders = new HttpHeaders();
        response.getHeaders().forEach((name, values) -> {
            String lowerName = name.toLowerCase();
            if (!SKIPPED_HEADERS.contains(lowerName)) {
                responseHeaders.put(name, values);
            }
        });
        return ResponseEntity
            .status(response.getStatusCode())
            .headers(responseHeaders)
            .body(response.getBody());
    }

    private String resolveProductTargetPath(String requestUri) {
        if (requestUri.startsWith("/admin/artist/score")) {
            return requestUri;
        }
        if (requestUri.startsWith("/api/admin/artist/score")) {
            return requestUri.substring("/api".length());
        }
        if (requestUri.startsWith("/api/artist/score")) {
            return "/admin" + requestUri.substring("/api".length());
        }
        return requestUri.substring("/api".length());
    }

    private String serviceUrl(String baseUrl, String path) {
        return baseUrl.replaceAll("/+$", "") + path;
    }

    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (SKIPPED_HEADERS.contains(name.toLowerCase())) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                headers.add(name, values.nextElement());
            }
        }
        return headers;
    }
}
