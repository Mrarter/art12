package com.shiyiju.auction.config;

import com.shiyiju.auction.websocket.AuctionWebSocketHub;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final AuctionWebSocketHub hub;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(hub, "/ws/auction/lot/{lotId}")
                .setAllowedOriginPatterns("https://a.art1.cn", "https://*.art1.cn", "http://localhost:*");
    }
}
