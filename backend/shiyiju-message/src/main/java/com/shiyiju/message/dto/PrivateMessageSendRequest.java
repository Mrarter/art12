package com.shiyiju.message.dto;

import lombok.Data;

@Data
public class PrivateMessageSendRequest {
    private Long recipientId;
    private String messageType;
    private String content;
    private String extraData;
}
