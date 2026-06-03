package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameConversationRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题长度不能超过128")
    private String title;
}
