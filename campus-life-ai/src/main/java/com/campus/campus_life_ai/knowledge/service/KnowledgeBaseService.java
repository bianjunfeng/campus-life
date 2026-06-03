package com.campus.campus_life_ai.knowledge.service;

import com.campus.campus_life_ai.knowledge.dto.CreateKnowledgeBaseRequest;
import com.campus.campus_life_ai.knowledge.dto.KnowledgeBaseDTO;
import com.campus.campus_life_ai.knowledge.dto.UpdateKnowledgeBaseRequest;
import com.campus.campus_life_ai.knowledge.entity.KnowledgeBase;
import com.campus.campus_life_ai.knowledge.mapper.KnowledgeBaseMapper;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeBaseService {

    static final String OWNER_USER = "USER";
    static final String VISIBILITY_PRIVATE = "PRIVATE";
    static final int STATUS_NORMAL = 0;

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeStorageProperties storageProperties;

    public KnowledgeBaseService(KnowledgeBaseMapper knowledgeBaseMapper,
                                KnowledgeStorageProperties storageProperties) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.storageProperties = storageProperties;
    }

    @Transactional
    public KnowledgeBaseDTO createUserBase(Long userId, CreateKnowledgeBaseRequest request) {
        int existingCount = knowledgeBaseMapper.countUserBases(userId);
        if (existingCount >= storageProperties.getMaxUserBases()) {
            throw new IllegalArgumentException("个人知识库数量已达上限");
        }

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setOwnerType(OWNER_USER);
        knowledgeBase.setOwnerId(userId);
        knowledgeBase.setName(request.getName().trim());
        knowledgeBase.setDescription(cleanDescription(request.getDescription()));
        knowledgeBase.setVisibility(VISIBILITY_PRIVATE);
        knowledgeBase.setStatus(STATUS_NORMAL);
        knowledgeBase.setDocumentCount(0);
        knowledgeBase.setTotalSize(0L);
        knowledgeBase.setCreatedBy(userId);
        knowledgeBase.setCreatedAt(LocalDateTime.now());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.insert(knowledgeBase);
        return toDTO(knowledgeBase);
    }

    public List<KnowledgeBaseDTO> listUserBases(Long userId) {
        return knowledgeBaseMapper.findUserBases(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    public KnowledgeBaseDTO getUserBase(Long userId, Long kbId) {
        return toDTO(requireUserBase(userId, kbId));
    }

    @Transactional
    public KnowledgeBaseDTO updateUserBase(Long userId, Long kbId, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = requireUserBase(userId, kbId);
        knowledgeBase.setName(request.getName().trim());
        knowledgeBase.setDescription(cleanDescription(request.getDescription()));
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateOwnedUserBase(knowledgeBase);
        return toDTO(requireUserBase(userId, kbId));
    }

    @Transactional
    public void deleteUserBase(Long userId, Long kbId) {
        int updated = knowledgeBaseMapper.markDeletedUserBase(kbId, userId);
        if (updated == 0) {
            throw new IllegalArgumentException("知识库不存在");
        }
    }

    public KnowledgeBase requireUserBase(Long userId, Long kbId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findOwnedUserBase(kbId, userId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        return knowledgeBase;
    }

    public long resolveUserStorage(Long userId) {
        Long total = knowledgeBaseMapper.sumUserStorage(userId);
        return total == null ? 0L : total;
    }

    public void refreshStats(Long kbId) {
        knowledgeBaseMapper.refreshStats(kbId);
    }

    KnowledgeBaseDTO toDTO(KnowledgeBase knowledgeBase) {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(knowledgeBase.getId());
        dto.setOwnerType(knowledgeBase.getOwnerType());
        dto.setName(knowledgeBase.getName());
        dto.setDescription(knowledgeBase.getDescription());
        dto.setVisibility(knowledgeBase.getVisibility());
        dto.setDocumentCount(knowledgeBase.getDocumentCount());
        dto.setTotalSize(knowledgeBase.getTotalSize());
        dto.setCreatedAt(knowledgeBase.getCreatedAt());
        dto.setUpdatedAt(knowledgeBase.getUpdatedAt());
        return dto;
    }

    private String cleanDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }
}
