package com.campus.campus_life_ai.knowledge.controller;

import com.campus.campus_life_ai.common.result.ApiResponse;
import com.campus.campus_life_ai.common.security.CurrentUserAccessor;
import com.campus.campus_life_ai.knowledge.dto.CreateKnowledgeBaseRequest;
import com.campus.campus_life_ai.knowledge.dto.KnowledgeBaseDTO;
import com.campus.campus_life_ai.knowledge.dto.KnowledgeDocumentDTO;
import com.campus.campus_life_ai.knowledge.dto.UpdateKnowledgeBaseRequest;
import com.campus.campus_life_ai.knowledge.service.KnowledgeBaseService;
import com.campus.campus_life_ai.knowledge.service.KnowledgeDocumentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/agent/knowledge-bases")
public class UserKnowledgeBaseController {

    private final CurrentUserAccessor currentUserAccessor;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentService knowledgeDocumentService;

    public UserKnowledgeBaseController(CurrentUserAccessor currentUserAccessor,
                                       KnowledgeBaseService knowledgeBaseService,
                                       KnowledgeDocumentService knowledgeDocumentService) {
        this.currentUserAccessor = currentUserAccessor;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    @PostMapping
    public ApiResponse<KnowledgeBaseDTO> create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(knowledgeBaseService.createUserBase(userId, request));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeBaseDTO>> list() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(knowledgeBaseService.listUserBases(userId));
    }

    @GetMapping("/{kbId}")
    public ApiResponse<KnowledgeBaseDTO> detail(@PathVariable Long kbId) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(knowledgeBaseService.getUserBase(userId, kbId));
    }

    @PatchMapping("/{kbId}")
    public ApiResponse<KnowledgeBaseDTO> update(@PathVariable Long kbId,
                                                @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(knowledgeBaseService.updateUserBase(userId, kbId, request));
    }

    @DeleteMapping("/{kbId}")
    public ApiResponse<Void> delete(@PathVariable Long kbId) {
        Long userId = currentUserAccessor.requireUserId();
        knowledgeBaseService.deleteUserBase(userId, kbId);
        return ApiResponse.success(null);
    }

    @PostMapping(value = "/{kbId}/documents", consumes = "multipart/form-data")
    public ApiResponse<KnowledgeDocumentDTO> uploadDocument(@PathVariable Long kbId,
                                                            @RequestParam("file") MultipartFile file) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(knowledgeDocumentService.uploadUserDocument(userId, kbId, file));
    }

    @GetMapping("/{kbId}/documents")
    public ApiResponse<List<KnowledgeDocumentDTO>> listDocuments(@PathVariable Long kbId) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(knowledgeDocumentService.listUserDocuments(userId, kbId));
    }

    @PostMapping("/{kbId}/documents/{documentId}/reindex")
    public ApiResponse<KnowledgeDocumentDTO> reindexDocument(@PathVariable Long kbId,
                                                             @PathVariable Long documentId) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(knowledgeDocumentService.reindexUserDocument(userId, kbId, documentId));
    }

    @DeleteMapping("/{kbId}/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long kbId,
                                            @PathVariable Long documentId) {
        Long userId = currentUserAccessor.requireUserId();
        knowledgeDocumentService.deleteUserDocument(userId, kbId, documentId);
        return ApiResponse.success(null);
    }
}
