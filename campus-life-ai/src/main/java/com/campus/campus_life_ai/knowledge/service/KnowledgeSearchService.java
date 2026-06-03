package com.campus.campus_life_ai.knowledge.service;

import com.campus.campus_life_ai.knowledge.dto.KnowledgeReferenceDTO;
import com.campus.campus_life_ai.knowledge.dto.KnowledgeSearchRequest;
import com.campus.campus_life_ai.knowledge.dto.KnowledgeSearchResultDTO;
import com.campus.campus_life_ai.knowledge.mapper.KnowledgeChunkMapper;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeVectorProperties;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeEmbeddingClient;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeVectorSearchHit;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10;

    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeVectorProperties vectorProperties;
    private final KnowledgeEmbeddingClient embeddingClient;
    private final KnowledgeVectorStore vectorStore;

    public KnowledgeSearchService(KnowledgeChunkMapper chunkMapper,
                                  KnowledgeVectorProperties vectorProperties,
                                  KnowledgeEmbeddingClient embeddingClient,
                                  KnowledgeVectorStore vectorStore) {
        this.chunkMapper = chunkMapper;
        this.vectorProperties = vectorProperties;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public List<KnowledgeSearchResultDTO> search(Long userId, KnowledgeSearchRequest request) {
        KnowledgePromptContext context = searchForAgent(
                userId,
                "chat.personal_qa",
                request.getQuery(),
                request.getKnowledgeBaseIds(),
                request.getUsePersonalKnowledge(),
                request.getUsePlatformKnowledge(),
                request.getTopK()
        );
        return context.getHits().stream()
                .map(this::toResultDTO)
                .toList();
    }

    public KnowledgePromptContext searchForAgent(Long userId,
                                                 String sceneCode,
                                                 String query,
                                                 List<Long> knowledgeBaseIds,
                                                 Boolean usePersonalKnowledge,
                                                 Boolean usePlatformKnowledge,
                                                 Integer topK) {
        boolean includePersonal = Boolean.TRUE.equals(usePersonalKnowledge)
                || (knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty())
                || "chat.personal_qa".equals(sceneCode)
                || "chat.mixed_qa".equals(sceneCode);
        boolean includePlatform = Boolean.TRUE.equals(usePlatformKnowledge)
                || "chat.campus_qa".equals(sceneCode)
                || "chat.mixed_qa".equals(sceneCode);

        KnowledgePromptContext context = new KnowledgePromptContext();
        if (!includePersonal && !includePlatform) {
            return context;
        }

        int limit = Math.min(Math.max(topK == null ? DEFAULT_TOP_K : topK, 1), MAX_TOP_K);
        List<KnowledgeSearchHit> vectorHits = searchByVector(
                userId,
                query,
                normalizeIds(knowledgeBaseIds),
                includePersonal,
                includePlatform,
                limit
        );
        if (!vectorHits.isEmpty()) {
            context.setHits(vectorHits);
            context.setReferences(vectorHits.stream().map(this::toReferenceDTO).toList());
            return context;
        }
        if (vectorProperties.isEnabled() && !vectorProperties.isFallbackKeywordSearch()) {
            return context;
        }

        List<String> keywords = tokenize(query);
        List<KnowledgeSearchHit> rawHits = chunkMapper.searchAccessibleChunks(
                userId,
                normalizeIds(knowledgeBaseIds),
                includePersonal,
                includePlatform,
                keywords,
                Math.max(limit * 4, limit)
        );

        List<KnowledgeSearchHit> ranked = rawHits.stream()
                .sorted(Comparator.comparingInt((KnowledgeSearchHit hit) -> score(hit, keywords)).reversed())
                .limit(limit)
                .toList();
        context.setHits(ranked);
        context.setReferences(ranked.stream().map(this::toReferenceDTO).toList());
        return context;
    }

    private KnowledgeSearchResultDTO toResultDTO(KnowledgeSearchHit hit) {
        KnowledgeSearchResultDTO dto = new KnowledgeSearchResultDTO();
        dto.setKbId(hit.getKbId());
        dto.setKnowledgeBaseName(hit.getKnowledgeBaseName());
        dto.setDocumentId(hit.getDocumentId());
        dto.setDocumentTitle(hit.getDocumentTitle());
        dto.setChunkIndex(hit.getChunkIndex());
        dto.setContent(hit.getContent());
        dto.setScore(hit.getScore() == null ? score(hit, tokenize(hit.getContent())) : hit.getScore());
        return dto;
    }

    private KnowledgeReferenceDTO toReferenceDTO(KnowledgeSearchHit hit) {
        KnowledgeReferenceDTO dto = new KnowledgeReferenceDTO();
        dto.setKbId(hit.getKbId());
        dto.setKnowledgeBaseName(hit.getKnowledgeBaseName());
        dto.setDocumentId(hit.getDocumentId());
        dto.setDocumentTitle(hit.getDocumentTitle());
        dto.setChunkIndex(hit.getChunkIndex());
        dto.setScore(hit.getScore());
        return dto;
    }

    private List<KnowledgeSearchHit> searchByVector(Long userId,
                                                    String query,
                                                    List<Long> knowledgeBaseIds,
                                                    boolean includePersonal,
                                                    boolean includePlatform,
                                                    int limit) {
        if (!vectorProperties.isEnabled()) {
            return List.of();
        }
        try {
            List<Float> queryVector = embeddingClient.embedOne(query);
            List<KnowledgeVectorSearchHit> vectorHits = vectorStore.search(
                    queryVector,
                    userId,
                    knowledgeBaseIds,
                    includePersonal,
                    includePlatform,
                    Math.max(limit * 4, limit)
            );
            if (vectorHits.isEmpty()) {
                return List.of();
            }

            List<String> vectorIds = vectorHits.stream()
                    .map(KnowledgeVectorSearchHit::getVectorId)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            if (vectorIds.isEmpty()) {
                return List.of();
            }

            Map<String, Integer> scores = vectorHits.stream()
                    .filter(hit -> StringUtils.hasText(hit.getVectorId()))
                    .collect(Collectors.toMap(
                            KnowledgeVectorSearchHit::getVectorId,
                            hit -> toScore(hit.getScore()),
                            (first, second) -> first,
                            LinkedHashMap::new
                    ));
            Map<String, KnowledgeSearchHit> chunksByVectorId = chunkMapper.findAccessibleChunksByVectorIds(
                            userId,
                            vectorIds,
                            knowledgeBaseIds,
                            includePersonal,
                            includePlatform
                    ).stream()
                    .collect(Collectors.toMap(
                            KnowledgeSearchHit::getVectorId,
                            Function.identity(),
                            (first, second) -> first
                    ));

            List<KnowledgeSearchHit> ranked = new ArrayList<>();
            for (String vectorId : vectorIds) {
                KnowledgeSearchHit hit = chunksByVectorId.get(vectorId);
                if (hit == null) {
                    continue;
                }
                hit.setScore(scores.get(vectorId));
                ranked.add(hit);
                if (ranked.size() >= limit) {
                    break;
                }
            }
            return ranked;
        } catch (Exception e) {
            if (!vectorProperties.isFallbackKeywordSearch()) {
                throw new IllegalStateException("知识库向量检索失败", e);
            }
            log.warn("知识库向量检索失败，回退到关键词检索: {}", e.getMessage());
            return List.of();
        }
    }

    private int toScore(Float score) {
        if (score == null) {
            return 0;
        }
        return Math.round(score * 10000);
    }

    private int score(KnowledgeSearchHit hit, List<String> keywords) {
        if (hit == null || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        String content = (hit.getContent() == null ? "" : hit.getContent()).toLowerCase(Locale.ROOT);
        String title = (hit.getDocumentTitle() == null ? "" : hit.getDocumentTitle()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String keyword : keywords) {
            String normalized = keyword.toLowerCase(Locale.ROOT);
            if (content.contains(normalized)) {
                score += 3;
            }
            if (title.contains(normalized)) {
                score += 5;
            }
        }
        return score;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private List<String> tokenize(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        String trimmed = query.trim();
        keywords.add(trimmed);
        for (String part : trimmed.split("[\\s,，。.!！？?;；:：、/\\\\()（）\\[\\]{}<>《》\"']+")) {
            String token = part.trim();
            if (token.length() >= 2) {
                keywords.add(token);
                addCharacterNgrams(keywords, token);
            }
        }
        if (keywords.size() == 1 && trimmed.length() > 8) {
            keywords.add(trimmed.substring(0, Math.min(trimmed.length(), 8)));
        }
        return new ArrayList<>(keywords);
    }

    private void addCharacterNgrams(Set<String> keywords, String token) {
        if (token.length() < 4 || !containsCjk(token)) {
            return;
        }
        int[] sizes = {4, 3, 2};
        for (int size : sizes) {
            if (token.length() < size) {
                continue;
            }
            for (int i = 0; i <= token.length() - size; i++) {
                keywords.add(token.substring(i, i + size));
            }
        }
    }

    private boolean containsCjk(String value) {
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(value.charAt(i));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}
