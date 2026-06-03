package com.campus.campus_life_ai.knowledge.controller;

import com.campus.campus_life_ai.common.result.ApiResponse;
import com.campus.campus_life_ai.common.security.CurrentUserAccessor;
import com.campus.campus_life_ai.knowledge.dto.KnowledgeSearchRequest;
import com.campus.campus_life_ai.knowledge.dto.KnowledgeSearchResultDTO;
import com.campus.campus_life_ai.knowledge.service.KnowledgeSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge")
public class KnowledgeSearchController {

    private final CurrentUserAccessor currentUserAccessor;
    private final KnowledgeSearchService knowledgeSearchService;

    public KnowledgeSearchController(CurrentUserAccessor currentUserAccessor,
                                     KnowledgeSearchService knowledgeSearchService) {
        this.currentUserAccessor = currentUserAccessor;
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @PostMapping("/search")
    public ApiResponse<List<KnowledgeSearchResultDTO>> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(knowledgeSearchService.search(userId, request));
    }
}
