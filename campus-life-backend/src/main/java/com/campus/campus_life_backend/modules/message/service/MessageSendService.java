package com.campus.campus_life_backend.modules.message.service;

import com.campus.campus_life_backend.modules.message.dto.MessageDTO;
import com.campus.campus_life_backend.modules.message.entity.Message;
import com.campus.campus_life_backend.modules.message.websocket.MessageRealtimeNotifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class MessageSendService {

    private final MessageService messageService;
    private final MessageRealtimeNotifier realtimeNotifier;

    public MessageSendService(MessageService messageService, MessageRealtimeNotifier realtimeNotifier) {
        this.messageService = messageService;
        this.realtimeNotifier = realtimeNotifier;
    }

    public MessageDTO sendMessage(Long fromUserId, Long toUserId, String content) {
        return sendMessage(fromUserId, toUserId, content, null, null);
    }

    public MessageDTO sendMessage(Long fromUserId,
                                  Long toUserId,
                                  String content,
                                  @Nullable String sourceSessionId,
                                  @Nullable String clientMessageId) {
        Message message = messageService.sendMessage(fromUserId, toUserId, content);
        MessageDTO dto = messageService.toMessageDTO(message);
        realtimeNotifier.publishChatMessage(dto, sourceSessionId, clientMessageId);
        return dto;
    }
}
