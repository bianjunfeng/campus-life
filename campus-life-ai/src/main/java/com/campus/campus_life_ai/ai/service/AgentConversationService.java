package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.AgentConversationDetailDTO;
import com.campus.campus_life_ai.ai.dto.AgentConversationSummaryDTO;
import com.campus.campus_life_ai.ai.dto.CreateConversationRequest;
import com.campus.campus_life_ai.ai.dto.RenameConversationRequest;
import com.campus.campus_life_ai.ai.entity.AiConversation;
import com.campus.campus_life_ai.ai.mapper.AiConversationMapper;
import com.campus.campus_life_ai.ai.mapper.AiMessageMapper;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AgentConversationService {

    private final AiConversationMapper aiConversationMapper;
    private final AiMessageMapper aiMessageMapper;
    private final MessageAssembler messageAssembler;
    private final AiProviderProperties providerProperties;

    public AgentConversationService(AiConversationMapper aiConversationMapper,
                                    AiMessageMapper aiMessageMapper,
                                    MessageAssembler messageAssembler,
                                    AiProviderProperties providerProperties) {
        this.aiConversationMapper = aiConversationMapper;
        this.aiMessageMapper = aiMessageMapper;
        this.messageAssembler = messageAssembler;
        this.providerProperties = providerProperties;
    }

    @Transactional
    public AgentConversationSummaryDTO createConversation(Long userId, CreateConversationRequest request) {
        AiConversation conversation = new AiConversation();
        conversation.setSessionId(generateSessionId());
        conversation.setUserId(userId);
        conversation.setAssistantType(StringUtils.hasText(request.getAssistantType())
                ? request.getAssistantType()
                : providerProperties.getDefaultAssistantType());
        conversation.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : "新对话");
        conversation.setStatus(0);
        conversation.setMessageCount(0);
        conversation.setPinned(0);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        aiConversationMapper.insert(conversation);
        return messageAssembler.toConversationSummary(conversation);
    }

    public List<AgentConversationSummaryDTO> listConversations(Long userId) {
        return aiConversationMapper.findByUserId(userId).stream()
                .map(messageAssembler::toConversationSummary)
                .toList();
    }

    public AgentConversationDetailDTO getConversationDetail(Long userId, String sessionId, Integer page, Integer pageSize) {
        AiConversation conversation = requireConversation(sessionId, userId);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 50 : Math.min(pageSize, 200);
        int offset = (safePage - 1) * safePageSize;

        AgentConversationDetailDTO detailDTO = new AgentConversationDetailDTO();
        detailDTO.setConversation(messageAssembler.toConversationSummary(conversation));
        detailDTO.setMessages(aiMessageMapper.findBySessionId(sessionId, offset, safePageSize).stream()
                .map(messageAssembler::toMessageDto)
                .toList());
        return detailDTO;
    }

    @Transactional
    public void renameConversation(Long userId, String sessionId, RenameConversationRequest request) {
        int updated = aiConversationMapper.updateTitle(sessionId, userId, request.getTitle().trim());
        if (updated == 0) {
            throw new IllegalArgumentException("会话不存在");
        }
    }

    @Transactional
    public void deleteConversation(Long userId, String sessionId) {
        int updated = aiConversationMapper.markDeleted(sessionId, userId);
        if (updated == 0) {
            throw new IllegalArgumentException("会话不存在");
        }
    }

    public AiConversation requireConversation(String sessionId, Long userId) {
        AiConversation conversation = aiConversationMapper.findBySessionIdAndUserId(sessionId, userId);
        if (conversation == null || conversation.getStatus() != null && conversation.getStatus() == 2) {
            throw new IllegalArgumentException("会话不存在");
        }
        return conversation;
    }

    public AiConversation findConversation(String sessionId, Long userId) {
        return aiConversationMapper.findBySessionIdAndUserId(sessionId, userId);
    }

    @Transactional
    public AiConversation createConversationIfAbsent(Long userId, String sessionId, String assistantType, String title) {
        return createConversationIfAbsent(userId, sessionId, assistantType, title, null, null, null, null);
    }

    @Transactional
    public AiConversation createConversationIfAbsent(Long userId, String sessionId, String assistantType, String title,
                                                     String capabilityCode, String sceneCode, String providerCode, String modelCode) {
        AiConversation existing = findConversation(sessionId, userId);
        if (existing != null) {
            return existing;
        }

        AiConversation conversation = new AiConversation();
        conversation.setSessionId(StringUtils.hasText(sessionId) ? sessionId : generateSessionId());
        conversation.setUserId(userId);
        conversation.setAssistantType(StringUtils.hasText(assistantType) ? assistantType : providerProperties.getDefaultAssistantType());
        conversation.setCapabilityCode(capabilityCode);
        conversation.setSceneCode(sceneCode);
        conversation.setProviderCode(providerCode);
        conversation.setModelCode(modelCode);
        conversation.setTitle(StringUtils.hasText(title) ? title : "新对话");
        conversation.setStatus(0);
        conversation.setMessageCount(0);
        conversation.setPinned(0);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        aiConversationMapper.insert(conversation);
        return conversation;
    }

    public String generateSessionId() {
        return "ses_" + UUID.randomUUID().toString().replace("-", "");
    }
}
