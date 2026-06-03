package com.campus.campus_life_ai.knowledge.service;

import com.campus.campus_life_ai.knowledge.dto.KnowledgeDocumentDTO;
import com.campus.campus_life_ai.knowledge.entity.KnowledgeBase;
import com.campus.campus_life_ai.knowledge.entity.KnowledgeChunk;
import com.campus.campus_life_ai.knowledge.entity.KnowledgeDocument;
import com.campus.campus_life_ai.knowledge.mapper.KnowledgeChunkMapper;
import com.campus.campus_life_ai.knowledge.mapper.KnowledgeDocumentMapper;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeStorageProperties;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeVectorProperties;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeEmbeddingClient;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeVectorRecord;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KnowledgeDocumentService {

    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_DELETED = 2;
    private static final int PARSE_PENDING = 0;
    private static final int PARSE_INDEXING = 1;
    private static final int PARSE_INDEXED = 2;
    private static final int PARSE_FAILED = 3;

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeStorageProperties storageProperties;
    private final DocumentParseService documentParseService;
    private final DocumentChunkService documentChunkService;
    private final KnowledgeHashService knowledgeHashService;
    private final KnowledgeVectorProperties vectorProperties;
    private final KnowledgeEmbeddingClient embeddingClient;
    private final KnowledgeVectorStore vectorStore;

    public KnowledgeDocumentService(KnowledgeBaseService knowledgeBaseService,
                                    KnowledgeDocumentMapper documentMapper,
                                    KnowledgeChunkMapper chunkMapper,
                                    KnowledgeStorageProperties storageProperties,
                                    DocumentParseService documentParseService,
                                    DocumentChunkService documentChunkService,
                                    KnowledgeHashService knowledgeHashService,
                                    KnowledgeVectorProperties vectorProperties,
                                    KnowledgeEmbeddingClient embeddingClient,
                                    KnowledgeVectorStore vectorStore) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.storageProperties = storageProperties;
        this.documentParseService = documentParseService;
        this.documentChunkService = documentChunkService;
        this.knowledgeHashService = knowledgeHashService;
        this.vectorProperties = vectorProperties;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public KnowledgeDocumentDTO uploadUserDocument(Long userId, Long kbId, MultipartFile file) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.requireUserBase(userId, kbId);
        validateFile(userId, file);

        String originalFilename = cleanFilename(file.getOriginalFilename());
        byte[] bytes = readBytes(file);
        Path storedPath = storeFile(userId, kbId, originalFilename, bytes);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setKbId(knowledgeBase.getId());
        document.setTitle(stripExtension(originalFilename));
        document.setOriginalFilename(originalFilename);
        document.setFilePath(storedPath.toString());
        document.setFileSize((long) bytes.length);
        document.setContentHash(knowledgeHashService.sha256(bytes));
        document.setStatus(STATUS_NORMAL);
        document.setParseStatus(PARSE_PENDING);
        document.setChunkCount(0);
        document.setCreatedBy(userId);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(document);

        indexDocument(knowledgeBase, document);
        knowledgeBaseService.refreshStats(kbId);
        return toDTO(documentMapper.findById(document.getId()));
    }

    public List<KnowledgeDocumentDTO> listUserDocuments(Long userId, Long kbId) {
        knowledgeBaseService.requireUserBase(userId, kbId);
        return documentMapper.findByKbId(kbId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public KnowledgeDocumentDTO reindexUserDocument(Long userId, Long kbId, Long documentId) {
        knowledgeBaseService.requireUserBase(userId, kbId);
        KnowledgeDocument document = requireDocument(kbId, documentId);
        KnowledgeBase knowledgeBase = knowledgeBaseService.requireUserBase(userId, kbId);
        deleteIndexedChunks(documentId);
        indexDocument(knowledgeBase, document);
        return toDTO(documentMapper.findById(documentId));
    }

    @Transactional
    public void deleteUserDocument(Long userId, Long kbId, Long documentId) {
        knowledgeBaseService.requireUserBase(userId, kbId);
        KnowledgeDocument document = requireDocument(kbId, documentId);
        int updated = documentMapper.markDeleted(document.getId(), kbId);
        if (updated == 0) {
            throw new IllegalArgumentException("文档不存在");
        }
        deleteIndexedChunks(documentId);
        knowledgeBaseService.refreshStats(kbId);
    }

    private KnowledgeDocument requireDocument(Long kbId, Long documentId) {
        KnowledgeDocument document = documentMapper.findActiveByIdAndKbId(documentId, kbId);
        if (document == null || STATUS_DELETED == safeInt(document.getStatus())) {
            throw new IllegalArgumentException("文档不存在");
        }
        return document;
    }

    private void indexDocument(KnowledgeBase knowledgeBase, KnowledgeDocument document) {
        updateParseState(document, PARSE_INDEXING, null, 0);
        try {
            String content = documentParseService.parse(Paths.get(document.getFilePath()));
            List<String> chunks = documentChunkService.chunk(content);
            deleteIndexedChunks(document.getId());

            List<KnowledgeChunk> chunkEntities = new java.util.ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setKbId(document.getKbId());
                chunk.setDocumentId(document.getId());
                chunk.setVectorId("kb-" + document.getKbId() + "-doc-" + document.getId() + "-chunk-" + i);
                chunk.setChunkIndex(i);
                chunk.setContent(chunks.get(i));
                chunk.setContentHash(knowledgeHashService.sha256(chunks.get(i)));
                chunk.setTokenCount(estimateTokenCount(chunks.get(i)));
                chunk.setCreatedAt(LocalDateTime.now());
                chunkEntities.add(chunk);
            }

            for (KnowledgeChunk chunk : chunkEntities) {
                chunkMapper.insert(chunk);
            }
            writeVectorIndex(knowledgeBase, chunkEntities);
            updateParseState(document, PARSE_INDEXED, null, chunks.size());
        } catch (Exception e) {
            cleanupFailedIndex(document.getId());
            updateParseState(document, PARSE_FAILED, abbreviate(e.getMessage(), 512), 0);
        }
    }

    private void writeVectorIndex(KnowledgeBase knowledgeBase, List<KnowledgeChunk> chunks) {
        if (!vectorProperties.isEnabled() || chunks.isEmpty()) {
            return;
        }
        List<List<Float>> embeddings = embeddingClient.embed(chunks.stream()
                .map(KnowledgeChunk::getContent)
                .toList());
        if (embeddings.size() != chunks.size()) {
            throw new IllegalStateException("向量化结果数量与分片数量不一致");
        }

        List<KnowledgeVectorRecord> records = new java.util.ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            records.add(KnowledgeVectorRecord.builder()
                    .knowledgeBase(knowledgeBase)
                    .chunk(chunks.get(i))
                    .embedding(embeddings.get(i))
                    .build());
        }
        vectorStore.upsert(records);
    }

    private void deleteIndexedChunks(Long documentId) {
        if (vectorProperties.isEnabled()) {
            vectorStore.deleteByDocumentId(documentId);
        }
        chunkMapper.deleteByDocumentId(documentId);
    }

    private void cleanupFailedIndex(Long documentId) {
        try {
            if (vectorProperties.isEnabled()) {
                vectorStore.deleteByDocumentId(documentId);
            }
        } catch (Exception ignored) {
            // Keep the parse failure visible even if Milvus cleanup is unavailable.
        }
        chunkMapper.deleteByDocumentId(documentId);
    }

    private void updateParseState(KnowledgeDocument document, int parseStatus, String errorMessage, int chunkCount) {
        document.setParseStatus(parseStatus);
        document.setErrorMessage(errorMessage);
        document.setChunkCount(chunkCount);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateParseState(document);
    }

    private void validateFile(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("文件大小超过限制");
        }
        long currentStorage = knowledgeBaseService.resolveUserStorage(userId);
        if (currentStorage + file.getSize() > storageProperties.getMaxUserStorageBytes()) {
            throw new IllegalArgumentException("个人知识库总容量已达上限");
        }

        String extension = getExtension(file.getOriginalFilename());
        Set<String> allowed = Arrays.stream(storageProperties.getAllowedExtensions().split(","))
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (!allowed.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持: " + storageProperties.getAllowedExtensions());
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取上传文件失败");
        }
    }

    private Path storeFile(Long userId, Long kbId, String originalFilename, byte[] bytes) {
        try {
            Path root = Paths.get(storageProperties.getRootPath()).toAbsolutePath().normalize();
            Path directory = root.resolve("user-" + userId).resolve("kb-" + kbId).normalize();
            if (!directory.startsWith(root)) {
                throw new IllegalArgumentException("文件路径非法");
            }
            Files.createDirectories(directory);
            String extension = getExtension(originalFilename);
            Path target = directory.resolve(UUID.randomUUID().toString().replace("-", "") + "." + extension).normalize();
            if (!target.startsWith(directory)) {
                throw new IllegalArgumentException("文件路径非法");
            }
            Files.write(target, bytes);
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("保存上传文件失败");
        }
    }

    private KnowledgeDocumentDTO toDTO(KnowledgeDocument document) {
        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();
        dto.setId(document.getId());
        dto.setKbId(document.getKbId());
        dto.setTitle(document.getTitle());
        dto.setOriginalFilename(document.getOriginalFilename());
        dto.setFileSize(document.getFileSize());
        dto.setStatus(document.getStatus());
        dto.setParseStatus(document.getParseStatus());
        dto.setParseStatusText(parseStatusText(document.getParseStatus()));
        dto.setErrorMessage(document.getErrorMessage());
        dto.setChunkCount(document.getChunkCount());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        return dto;
    }

    private String parseStatusText(Integer status) {
        return switch (safeInt(status)) {
            case PARSE_PENDING -> "PENDING";
            case PARSE_INDEXING -> "INDEXING";
            case PARSE_INDEXED -> "INDEXED";
            case PARSE_FAILED -> "FAILED";
            default -> "UNKNOWN";
        };
    }

    private int safeInt(Integer value) {
        return value == null ? -1 : value;
    }

    private String cleanFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "document.txt";
        }
        return Paths.get(filename).getFileName().toString().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return index <= 0 ? filename : filename.substring(0, index);
    }

    private int estimateTokenCount(String content) {
        return content == null ? 0 : Math.max(1, content.length() / 2);
    }

    private String abbreviate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
