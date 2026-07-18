package com.shiyiju.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyiju.message.entity.PrivateMessage;
import com.shiyiju.message.vo.ConversationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    @Select("""
            SELECT peer.id AS peer_id,
                   COALESCE(NULLIF(peer.nickname, ''), CONCAT('用户 ', peer.id)) AS peer_name,
                   peer.avatar AS peer_avatar,
                   peer.identities AS peer_identities,
                   last_message.id AS last_message_id,
                   last_message.sender_id AS last_sender_id,
                   last_message.message_type AS last_message_type,
                   last_message.content AS last_content,
                   last_message.extra_data AS last_extra_data,
                   last_message.create_time AS last_time,
                   (SELECT COUNT(*)
                      FROM private_messages unread
                     WHERE unread.sender_id = peer.id
                       AND unread.recipient_id = #{userId}
                       AND unread.is_read = 0) AS unread_count
              FROM (
                    SELECT CASE WHEN sender_id = #{userId} THEN recipient_id ELSE sender_id END AS peer_id,
                           MAX(id) AS last_message_id
                      FROM private_messages
                     WHERE sender_id = #{userId} OR recipient_id = #{userId}
                     GROUP BY CASE WHEN sender_id = #{userId} THEN recipient_id ELSE sender_id END
                   ) conversation
              JOIN private_messages last_message ON last_message.id = conversation.last_message_id
              JOIN users peer ON peer.id = conversation.peer_id AND peer.deleted = 0
             ORDER BY last_message.id DESC
             LIMIT #{offset}, #{pageSize}
            """)
    List<ConversationVO> selectConversations(
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize
    );

    @Select("""
            SELECT COUNT(*) FROM (
                SELECT CASE WHEN sender_id = #{userId} THEN recipient_id ELSE sender_id END AS peer_id
                  FROM private_messages
                 WHERE sender_id = #{userId} OR recipient_id = #{userId}
                 GROUP BY CASE WHEN sender_id = #{userId} THEN recipient_id ELSE sender_id END
            ) conversations
            """)
    long countConversations(@Param("userId") Long userId);
}
