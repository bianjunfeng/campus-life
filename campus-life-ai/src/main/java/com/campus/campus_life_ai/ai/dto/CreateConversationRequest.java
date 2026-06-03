package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConversationRequest {
    @Size(max = 32, message = "助手类型长度不能超过32")
    private String assistantType;

    @Size(max = 128, message = "标题长度不能超过128")
    private String title;
}
