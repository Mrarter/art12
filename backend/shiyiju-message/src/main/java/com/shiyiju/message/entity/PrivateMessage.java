package com.shiyiju.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("private_messages")
public class PrivateMessage implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private Long recipientId;
    private String messageType;
    private String content;
    private String extraData;
    private Integer isRead;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
