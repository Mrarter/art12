package com.shiyiju.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private Long peerId;
    private String peerName;
    private String peerAvatar;
    private String peerIdentities;
    private Long lastMessageId;
    private Long lastSenderId;
    private String lastMessageType;
    private String lastContent;
    private String lastExtraData;
    private LocalDateTime lastTime;
    private Long unreadCount;
}
