package com.shiyiju.auction.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AuctionWebSocketHub extends TextWebSocketHandler {
    private final Map<Long, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long lotId = lotId(session);
        if (lotId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        session.getAttributes().put("lotId", lotId);
        rooms.computeIfAbsent(lotId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object value = session.getAttributes().get("lotId");
        if (value instanceof Long lotId) {
            Set<WebSocketSession> room = rooms.get(lotId);
            if (room != null) {
                room.remove(session);
                if (room.isEmpty()) rooms.remove(lotId);
            }
        }
    }

    public void broadcast(Long lotId, Map<String, Object> data) {
        Set<WebSocketSession> room = rooms.get(lotId);
        if (room == null || room.isEmpty()) return;
        TextMessage message = new TextMessage(JSON.toJSONString(data));
        room.forEach(session -> {
            if (!session.isOpen()) return;
            try {
                synchronized (session) { session.sendMessage(message); }
            } catch (IOException e) {
                log.warn("拍卖实时消息发送失败: lotId={}, session={}", lotId, session.getId());
            }
        });
    }

    private Long lotId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String path = uri.getPath();
        try { return Long.valueOf(path.substring(path.lastIndexOf('/') + 1)); }
        catch (RuntimeException ignored) { return null; }
    }
}
