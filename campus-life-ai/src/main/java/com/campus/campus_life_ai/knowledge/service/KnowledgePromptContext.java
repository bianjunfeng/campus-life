package com.campus.campus_life_ai.knowledge.service;

import com.campus.campus_life_ai.knowledge.dto.KnowledgeReferenceDTO;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgePromptContext {
    private List<KnowledgeSearchHit> hits = new ArrayList<>();
    private List<KnowledgeReferenceDTO> references = new ArrayList<>();

    public boolean hasContext() {
        return hits != null && !hits.isEmpty();
    }

    public String buildPrompt(String question) {
        if (!hasContext()) {
            return question;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("请优先依据以下知识库资料回答用户问题；如果资料不足，请明确说明不足之处，不要编造。\n\n");
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeSearchHit hit = hits.get(i);
            builder.append("【资料").append(i + 1).append("】");
            if (StringUtils.hasText(hit.getDocumentTitle())) {
                builder.append(hit.getDocumentTitle());
            }
            builder.append("\n");
            builder.append(hit.getContent()).append("\n\n");
        }
        builder.append("用户问题：").append(question);
        return builder.toString();
    }
}
