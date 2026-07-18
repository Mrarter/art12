package com.shiyiju.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.message.dto.PrivateMessageSendRequest;
import com.shiyiju.message.entity.Message;
import com.shiyiju.message.entity.PrivateMessage;
import com.shiyiju.message.mapper.MessageMapper;
import com.shiyiju.message.mapper.PrivateMessageMapper;
import com.shiyiju.message.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageMapper messageMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final Set<String> PRIVATE_MESSAGE_TYPES = Set.of("text", "image", "work", "order");

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

    /** 获取当前用户的真实私信会话列表 */
    @GetMapping("/chat/conversations")
    public Result<PageResult<ConversationVO>> getConversations(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        long total = privateMessageMapper.countConversations(userId);
        List<ConversationVO> records = privateMessageMapper.selectConversations(
                userId,
                (long) (safePage - 1) * safePageSize,
                safePageSize
        );
        return Result.success(PageResult.of(total, safePage, safePageSize, records));
    }

    /** 获取与指定用户的私信历史，返回时间正序 */
    @GetMapping("/chat/history/{peerId}")
    public Result<PageResult<PrivateMessage>> getChatHistory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long peerId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "30") Integer pageSize
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<PrivateMessage> wrapper = conversationWrapper(userId, peerId)
                .orderByDesc(PrivateMessage::getId);
        Page<PrivateMessage> result = privateMessageMapper.selectPage(
                new Page<>(safePage, safePageSize),
                wrapper
        );
        List<PrivateMessage> records = result.getRecords();
        Collections.reverse(records);
        return Result.success(PageResult.of(result.getTotal(), safePage, safePageSize, records));
    }

    /** 发送真实私信 */
    @Transactional
    @PostMapping("/chat/send")
    public Result<PrivateMessage> sendPrivateMessage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody PrivateMessageSendRequest request
    ) {
        if (request.getRecipientId() == null || request.getRecipientId() <= 0) {
            return Result.fail(400, "接收用户无效");
        }
        if (userId.equals(request.getRecipientId())) {
            return Result.fail(400, "不能给自己发送私信");
        }
        String type = request.getMessageType() == null ? "text" : request.getMessageType().trim().toLowerCase();
        if (!PRIVATE_MESSAGE_TYPES.contains(type)) {
            return Result.fail(400, "不支持的消息类型");
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty() && "text".equals(type)) {
            return Result.fail(400, "消息内容不能为空");
        }
        if (content.length() > 4000) {
            return Result.fail(400, "消息内容过长");
        }
        Integer recipientExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? AND deleted = 0 AND status = 1",
                Integer.class,
                request.getRecipientId()
        );
        if (recipientExists == null || recipientExists == 0) {
            return Result.fail(404, "接收用户不存在");
        }

        PrivateMessage message = new PrivateMessage();
        message.setSenderId(userId);
        message.setRecipientId(request.getRecipientId());
        message.setMessageType(type);
        message.setContent(content);
        message.setExtraData(request.getExtraData());
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        privateMessageMapper.insert(message);
        return Result.success(message);
    }

    /** 标记来自指定用户的消息已读 */
    @Transactional
    @PutMapping("/chat/read/{peerId}")
    public Result<Void> markChatRead(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long peerId
    ) {
        privateMessageMapper.update(null,
                new LambdaUpdateWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getSenderId, peerId)
                        .eq(PrivateMessage::getRecipientId, userId)
                        .eq(PrivateMessage::getIsRead, 0)
                        .set(PrivateMessage::getIsRead, 1)
                        .set(PrivateMessage::getReadTime, LocalDateTime.now())
        );
        return Result.success();
    }

    /** 获取私信未读总数 */
    @GetMapping("/chat/unread-count")
    public Result<Long> getChatUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        long count = privateMessageMapper.selectCount(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getRecipientId, userId)
                        .eq(PrivateMessage::getIsRead, 0)
        );
        return Result.success(count);
    }

    private LambdaQueryWrapper<PrivateMessage> conversationWrapper(Long userId, Long peerId) {
        return new LambdaQueryWrapper<PrivateMessage>()
                .and(wrapper -> wrapper
                        .eq(PrivateMessage::getSenderId, userId)
                        .eq(PrivateMessage::getRecipientId, peerId)
                        .or()
                        .eq(PrivateMessage::getSenderId, peerId)
                        .eq(PrivateMessage::getRecipientId, userId));
    }
}
