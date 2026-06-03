package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SendMessageRequest {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容长度不能超过2000")
    private String content;

    @Size(max = 32, message = "能力编码长度不能超过32")
    private String capabilityCode;

    @Size(max = 64, message = "场景编码长度不能超过64")
    private String sceneCode;

    @Size(max = 32, message = "供应商编码长度不能超过32")
    private String providerCode;

    @Size(max = 64, message = "模型编码长度不能超过64")
    private String modelCode;

    private List<Long> knowledgeBaseIds;

    private Boolean usePersonalKnowledge;

    private Boolean usePlatformKnowledge;
}
