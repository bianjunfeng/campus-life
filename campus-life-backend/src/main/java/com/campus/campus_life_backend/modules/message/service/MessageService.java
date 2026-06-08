package com.campus.campus_life_backend.modules.message.service;

import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.modules.message.dto.ConversationDTO;
import com.campus.campus_life_backend.modules.message.dto.MessageDTO;
import com.campus.campus_life_backend.modules.message.entity.Message;
import com.campus.campus_life_backend.modules.message.mapper.MessageMapper;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequirePermission(anyOf = {"message:use"})
public class MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    public MessageService(MessageMapper messageMapper, UserMapper userMapper) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
    }

    /**
     * 鐢熸垚浼氳瘽ID锛堝皬ID_澶D鏍煎紡锛?
     */
    private String generateConversationId(Long userId1, Long userId2) {
        if (userId1 < userId2) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    /**
     * 鍙戦€佹秷鎭?
     */
    @Transactional
    public Message sendMessage(Long fromUserId, Long toUserId, String content) {
        // 妫€鏌ユ槸鍚︽槸鑷繁缁欒嚜宸卞彂娑堟伅
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("涓嶈兘缁欒嚜宸卞彂娑堟伅");
        }

        User toUser = userMapper.findById(toUserId);
        if (toUser == null || toUser.getStatus() == null || toUser.getStatus() != 1) {
            throw new IllegalArgumentException("接收方用户不存在或不可用");
        }

        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (normalizedContent.length() > 2000) {
            throw new IllegalArgumentException("消息内容长度不能超过2000字符");
        }

        // 鐢熸垚鎴栬幏鍙栦細璇滻D
        String conversationId = messageMapper.findConversationId(fromUserId, toUserId);
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = generateConversationId(fromUserId, toUserId);
        }

        // 鍒涘缓娑堟伅
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(normalizedContent);
        message.setStatus(0);  // 鏈
        message.setCreateTime(LocalDateTime.now());

        messageMapper.insertMessage(message);
        return message;
    }

    /**
     * 鑾峰彇浼氳瘽鐨勬秷鎭垪琛?
     */
    public List<MessageDTO> getMessagesByConversationId(String conversationId, Long currentUserId, Integer page, Integer pageSize) {
        validateConversationAccess(conversationId, currentUserId);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 50 : Math.min(pageSize, 200);
        Integer offset = (safePage - 1) * safePageSize;
        List<Message> messages = messageMapper.findByConversationId(conversationId, offset, safePageSize);
        return toMessageDTOs(messages);
    }

    public List<MessageDTO> syncMessagesByConversationId(String conversationId,
                                                         Long currentUserId,
                                                         Long beforeId,
                                                         Long afterId,
                                                         Integer limit) {
        validateConversationAccess(conversationId, currentUserId);
        return queryMessagesByCursor(conversationId, beforeId, afterId, limit);
    }

    /**
     * 通过对方用户ID同步会话消息，允许尚无消息的空会话。
     */
    public List<MessageDTO> syncMessagesByUserId(Long currentUserId,
                                                 Long otherUserId,
                                                 Long beforeId,
                                                 Long afterId,
                                                 Integer limit) {
        if (currentUserId == null || otherUserId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (currentUserId.equals(otherUserId)) {
            throw new IllegalArgumentException("不能与自己会话");
        }

        User otherUser = userMapper.findById(otherUserId);
        if (otherUser == null || otherUser.getStatus() == null || otherUser.getStatus() != 1) {
            throw new IllegalArgumentException("目标用户不存在或不可用");
        }

        String existingConversationId = messageMapper.findConversationId(currentUserId, otherUserId);
        if (existingConversationId == null || existingConversationId.isEmpty()) {
            return new ArrayList<>();
        }
        return queryMessagesByCursor(existingConversationId, beforeId, afterId, limit);
    }

    public MessageDTO toMessageDTO(Message msg) {
        if (msg == null) {
            return null;
        }
        Map<Long, User> usersById = loadUsersByIds(extractUserIds(List.of(msg)));
        return toMessageDTO(msg, usersById);
    }

    /**
     * 鑾峰彇鐢ㄦ埛鐨勪細璇濆垪琛?
     */
    public List<ConversationDTO> getConversationsByUserId(Long userId) {
        List<Message> latestMessages = deduplicateLatestMessages(messageMapper.findConversationsByUserId(userId));
        List<ConversationDTO> conversations = new ArrayList<>();
        List<Long> otherUserIds = new ArrayList<>();
        List<String> conversationIds = new ArrayList<>();

        for (Message msg : latestMessages) {
            Long otherUserId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            otherUserIds.add(otherUserId);
            conversationIds.add(msg.getConversationId());
        }

        Map<Long, User> usersById = loadUsersByIds(otherUserIds);
        Map<String, Integer> unreadCountByConversation = loadUnreadCountByConversationIds(userId, conversationIds);

        for (Message msg : latestMessages) {
            ConversationDTO dto = new ConversationDTO();
            dto.setConversationId(msg.getConversationId());
            dto.setLastMessage(msg.getContent());
            dto.setLastMessageTime(msg.getCreateTime());

            // 纭畾瀵规柟鐢ㄦ埛ID
            Long otherUserId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            dto.setOtherUserId(otherUserId);

            User otherUser = usersById.get(otherUserId);
            if (otherUser != null) {
                dto.setOtherUserName(otherUser.getUsername());
                dto.setOtherUserAvatar(otherUser.getAvatarUrl());
            }

            dto.setUnreadCount(unreadCountByConversation.getOrDefault(msg.getConversationId(), 0));

            conversations.add(dto);
        }

        return conversations;
    }

    private List<Message> deduplicateLatestMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        Map<String, Message> latestByConversation = new HashMap<>();
        for (Message message : messages) {
            if (message == null || message.getConversationId() == null || message.getConversationId().isBlank()) {
                continue;
            }
            latestByConversation.merge(message.getConversationId(), message, this::pickLatestMessage);
        }

        return latestByConversation.values().stream()
                .sorted(this::compareMessageDesc)
                .collect(Collectors.toList());
    }

    private List<MessageDTO> queryMessagesByCursor(String conversationId, Long beforeId, Long afterId, Integer limit) {
        if (beforeId != null && afterId != null) {
            throw new IllegalArgumentException("beforeId和afterId不能同时传入");
        }

        int safeLimit = limit == null || limit < 1 ? 100 : Math.min(limit, 200);
        List<Message> messages;
        if (afterId != null) {
            messages = messageMapper.findAfterIdByConversationId(conversationId, afterId, safeLimit);
        } else if (beforeId != null) {
            messages = messageMapper.findBeforeIdByConversationId(conversationId, beforeId, safeLimit);
        } else {
            messages = messageMapper.findLatestByConversationId(conversationId, safeLimit);
        }
        return toMessageDTOs(messages);
    }

    private List<MessageDTO> toMessageDTOs(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersById = loadUsersByIds(extractUserIds(messages));
        return messages.stream()
                .map(msg -> toMessageDTO(msg, usersById))
                .collect(Collectors.toList());
    }

    private MessageDTO toMessageDTO(Message msg, Map<Long, User> usersById) {
        MessageDTO dto = new MessageDTO();
        dto.setId(msg.getId());
        dto.setConversationId(msg.getConversationId());
        dto.setFromUserId(msg.getFromUserId());
        dto.setToUserId(msg.getToUserId());
        dto.setContent(msg.getContent());
        dto.setStatus(msg.getStatus());
        dto.setCreateTime(msg.getCreateTime());

        User fromUser = usersById.get(msg.getFromUserId());
        if (fromUser != null) {
            dto.setFromUserName(fromUser.getUsername());
            dto.setFromUserAvatar(fromUser.getAvatarUrl());
        }

        User toUser = usersById.get(msg.getToUserId());
        if (toUser != null) {
            dto.setToUserName(toUser.getUsername());
            dto.setToUserAvatar(toUser.getAvatarUrl());
        }

        return dto;
    }

    private Message pickLatestMessage(Message current, Message candidate) {
        return compareMessageDesc(current, candidate) <= 0 ? current : candidate;
    }

    private int compareMessageDesc(Message left, Message right) {
        Comparator<LocalDateTime> timeComparator = Comparator.nullsFirst(LocalDateTime::compareTo);
        Comparator<Long> idComparator = Comparator.nullsFirst(Long::compareTo);

        int timeCompare = timeComparator.compare(right.getCreateTime(), left.getCreateTime());
        if (timeCompare != 0) {
            return timeCompare;
        }
        return idComparator.compare(right.getId(), left.getId());
    }

    /**
     * 鑾峰彇鎴栧垱寤轰細璇滻D
     */
    public String getOrCreateConversationId(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("不能与自己创建会话");
        }
        User otherUser = userMapper.findById(userId2);
        if (otherUser == null || otherUser.getStatus() == null || otherUser.getStatus() != 1) {
            throw new IllegalArgumentException("目标用户不存在或不可用");
        }
        String conversationId = messageMapper.findConversationId(userId1, userId2);
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = generateConversationId(userId1, userId2);
        }
        return conversationId;
    }

    /**
     * 通过对方用户ID获取会话消息（允许空会话）
     */
    public List<MessageDTO> getMessagesByUserId(Long currentUserId, Long otherUserId, Integer page, Integer pageSize) {
        if (currentUserId == null || otherUserId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (currentUserId.equals(otherUserId)) {
            throw new IllegalArgumentException("不能与自己会话");
        }

        User otherUser = userMapper.findById(otherUserId);
        if (otherUser == null || otherUser.getStatus() == null || otherUser.getStatus() != 1) {
            throw new IllegalArgumentException("目标用户不存在或不可用");
        }

        String existingConversationId = messageMapper.findConversationId(currentUserId, otherUserId);
        if (existingConversationId == null || existingConversationId.isEmpty()) {
            return new ArrayList<>();
        }
        return getMessagesByConversationId(existingConversationId, currentUserId, page, pageSize);
    }

    /**
     * 鏍囪浼氳瘽娑堟伅涓哄凡璇?
     */
    @Transactional
    public void markConversationAsRead(String conversationId, Long userId) {
        validateConversationAccess(conversationId, userId);
        messageMapper.markAsRead(conversationId, userId);
    }

    /**
     * 鑾峰彇鐢ㄦ埛鏈娑堟伅鎬绘暟
     */
    public int getUnreadCount(Long userId) {
        return messageMapper.countUnreadByUserId(userId);
    }

    private void validateConversationAccess(String conversationId, Long userId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        int count = messageMapper.existsConversationForUser(conversationId, userId);
        if (count <= 0) {
            throw new IllegalArgumentException("无权访问该会话");
        }
    }

    private List<Long> extractUserIds(List<Message> messages) {
        Set<Long> userIds = new HashSet<>();
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        for (Message message : messages) {
            if (message.getFromUserId() != null) {
                userIds.add(message.getFromUserId());
            }
            if (message.getToUserId() != null) {
                userIds.add(message.getToUserId());
            }
        }
        return new ArrayList<>(userIds);
    }

    private Map<Long, User> loadUsersByIds(List<Long> userIds) {
        Map<Long, User> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        List<User> users = userMapper.findByIds(userIds);
        if (users != null) {
            for (User user : users) {
                result.put(user.getId(), user);
            }
            return result;
        }

        for (Long userId : userIds) {
            User user = userMapper.findById(userId);
            if (user != null) {
                result.put(userId, user);
            }
        }
        return result;
    }

    private Map<String, Integer> loadUnreadCountByConversationIds(Long userId, List<String> conversationIds) {
        Map<String, Integer> result = new HashMap<>();
        if (userId == null || conversationIds == null || conversationIds.isEmpty()) {
            return result;
        }

        List<Map<String, Object>> unreadRows = messageMapper.countUnreadByConversationIds(userId, conversationIds);
        if (unreadRows != null) {
            for (Map<String, Object> row : unreadRows) {
                if (row == null) {
                    continue;
                }
                Object conversationId = row.get("conversationId");
                Object unreadCount = row.get("unreadCount");
                if (conversationId == null || !(unreadCount instanceof Number number)) {
                    continue;
                }
                result.put(String.valueOf(conversationId), number.intValue());
            }
            return result;
        }

        for (String conversationId : conversationIds) {
            result.put(conversationId, messageMapper.countUnreadByConversationId(conversationId, userId));
        }
        return result;
    }
}

