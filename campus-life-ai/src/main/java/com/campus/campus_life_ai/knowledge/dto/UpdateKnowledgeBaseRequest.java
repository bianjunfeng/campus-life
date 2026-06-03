package com.campus.campus_life_ai.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateKnowledgeBaseRequest {
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 128, message = "知识库名称不能超过128个字符")
    private String name;

    @Size(max = 512, message = "知识库描述不能超过512个字符")
    private String description;
}
