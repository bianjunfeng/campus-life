package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.modules.message.dto.ConversationDTO;
import com.campus.campus_life_backend.modules.message.entity.Message;
import com.campus.campus_life_backend.modules.message.mapper.MessageMapper;
import com.campus.campus_life_backend.modules.message.service.MessageService;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MessageService messageService;

    @Test
    void shouldDeduplicateConversationListAndKeepLatestMessageWhenTimestampsTie() {
        LocalDateTime sameTime = LocalDateTime.of(2026, 3, 20, 10, 0, 0);

        Message duplicateOlder = buildMessage(10L, "1_2", 1L, 2L, "old-message", sameTime);
        Message duplicateLatest = buildMessage(11L, "1_2", 2L, 1L, "new-message", sameTime);
        Message anotherConversation = buildMessage(12L, "1_3", 3L, 1L, "another-conversation", sameTime.minusMinutes(5));
        Map<String, Object> unreadConversation1 = new HashMap<>();
        unreadConversation1.put("conversationId", "1_2");
        unreadConversation1.put("unreadCount", 2);
        Map<String, Object> unreadConversation2 = new HashMap<>();
        unreadConversation2.put("conversationId", "1_3");
        unreadConversation2.put("unreadCount", 0);

        when(messageMapper.findConversationsByUserId(1L)).thenReturn(List.of(duplicateOlder, duplicateLatest, anotherConversation));
        when(messageMapper.countUnreadByConversationIds(eq(1L), anyList())).thenReturn(List.of(unreadConversation1, unreadConversation2));
        when(userMapper.findByIds(anyList())).thenReturn(List.of(buildUser(2L, "user-2"), buildUser(3L, "user-3")));

        List<ConversationDTO> conversations = messageService.getConversationsByUserId(1L);

        assertEquals(2, conversations.size());
        assertIterableEquals(List.of("1_2", "1_3"), conversations.stream().map(ConversationDTO::getConversationId).toList());
        assertEquals("new-message", conversations.get(0).getLastMessage());
        assertEquals(2, conversations.get(0).getUnreadCount());
    }

    private Message buildMessage(Long id, String conversationId, Long fromUserId, Long toUserId, String content, LocalDateTime createTime) {
        Message message = new Message();
        message.setId(id);
        message.setConversationId(conversationId);
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setStatus(0);
        message.setCreateTime(createTime);
        return message;
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setAvatarUrl("avatar-" + id);
        return user;
    }
}
