package com.campus.campus_life_ai.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeSearchRequest {
    @NotBlank(message = "搜索内容不能为空")
    @Size(max = 2000, message = "搜索内容不能超过2000个字符")
    private String query;

    private List<Long> knowledgeBaseIds;
    private Boolean usePersonalKnowledge;
    private Boolean usePlatformKnowledge;
    private Integer topK;
}
