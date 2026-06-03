package com.campus.campus_life_ai.knowledge.service;

import com.campus.campus_life_ai.knowledge.properties.KnowledgeStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkService {

    private final KnowledgeStorageProperties storageProperties;

    public DocumentChunkService(KnowledgeStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public List<String> chunk(String content) {
        String normalized = normalize(content);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        int maxSize = Math.max(storageProperties.getChunkMaxSize(), 200);
        int overlap = Math.max(Math.min(storageProperties.getChunkOverlap(), maxSize / 2), 0);
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxSize, normalized.length());
            int adjustedEnd = adjustEnd(normalized, start, end);
            String chunk = normalized.substring(start, adjustedEnd).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            if (adjustedEnd >= normalized.length()) {
                break;
            }
            start = Math.max(adjustedEnd - overlap, start + 1);
        }
        return chunks;
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private int adjustEnd(String content, int start, int end) {
        if (end >= content.length()) {
            return content.length();
        }
        int paragraphBreak = content.lastIndexOf("\n\n", end);
        if (paragraphBreak > start + 100) {
            return paragraphBreak;
        }
        int lineBreak = content.lastIndexOf('\n', end);
        if (lineBreak > start + 100) {
            return lineBreak;
        }
        int sentenceBreak = Math.max(content.lastIndexOf('。', end), content.lastIndexOf('.', end));
        if (sentenceBreak > start + 100) {
            return sentenceBreak + 1;
        }
        return end;
    }
}
