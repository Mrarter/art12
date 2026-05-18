package com.shiyiju.gateway.filter;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 将 AuthFilter 解析的用户 ID 写入转发的请求头
 * 使用 ServerHttpRequestDecorator 保留请求体
 */
@Component
public class XUserIdGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public XUserIdGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public String name() {
        return "XUserId";
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            Long userId = exchange.getAttribute(AuthFilter.USER_ID_ATTR);
            if (userId != null) {
                ServerHttpRequest original = exchange.getRequest();
                ServerHttpRequest decorated = new ServerHttpRequestDecorator(original) {
                    @Override
                    public HttpHeaders getHeaders() {
                        HttpHeaders headers = new HttpHeaders();
                        headers.addAll(super.getHeaders());
                        headers.set("X-User-Id", String.valueOf(userId));
                        return headers;
                    }

                    @Override
                    public Flux<DataBuffer> getBody() {
                        Flux<DataBuffer> body = super.getBody();
                        return body;
                    }
                };
                return chain.filter(exchange.mutate().request(decorated).build());
            }
            return chain.filter(exchange);
        };
    }
}
