package com.shiyiju.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.message.entity.Message;
import com.shiyiju.message.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageMapper messageMapper;

    /** 获取消息列表 */
    @GetMapping("/list")
    public Result<PageResult<Message>> getMessages(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId);
        if (type != null) {
            wrapper.eq(Message::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(Message::getIsRead, isRead);
        }
        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> result = messageMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return Result.success(PageResult.of(result.getTotal(), page, pageSize, result.getRecords()));
    }

    /** 获取未读消息数量 */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        long count = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getIsRead, 0)
        );
        return Result.success(count);
    }

    /** 标记消息已读 */
    @Transactional
    @PutMapping("/read/{messageId}")
    public Result<Void> markAsRead(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long messageId
    ) {
        Message message = messageMapper.selectOne(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getId, messageId)
                        .eq(Message::getUserId, userId)
        );
        if (message != null) {
            message.setIsRead(1);
            message.setReadTime(LocalDateTime.now());
            messageMapper.updateById(message);
        }
        return Result.success();
    }

    /** 标记所有消息已读 */
    @Transactional
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        Message update = new Message();
        update.setIsRead(1);
        update.setReadTime(LocalDateTime.now());
        messageMapper.update(update,
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getIsRead, 0)
        );
        return Result.success();
    }

    /** 删除消息 */
    @Transactional
    @DeleteMapping("/{messageId}")
    public Result<Void> deleteMessage(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long messageId
    ) {
        messageMapper.delete(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getId, messageId)
                        .eq(Message::getUserId, userId)
        );
        return Result.success();
    }
}
