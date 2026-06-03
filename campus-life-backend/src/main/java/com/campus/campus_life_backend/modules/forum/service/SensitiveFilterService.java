package com.campus.campus_life_backend.modules.forum.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

@Service
public class SensitiveFilterService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveFilterService.class);
    private static final String REPLACEMENT = "***";
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 5000;

    private final Path managedWordFile;
    private final long reloadCheckMs;
    private final boolean hotReloadEnabled;
    private final AtomicReference<DictionarySnapshot> snapshotRef = new AtomicReference<>(DictionarySnapshot.empty());
    private final AtomicLong versionCounter = new AtomicLong();
    private final Object mutationMonitor = new Object();

    @Autowired
    public SensitiveFilterService(
            @Value("${sensitive.filter.file-path:}") String configuredFilePath,
            @Value("${file.upload.path:uploads}") String uploadPath,
            @Value("${sensitive.filter.reload-check-ms:5000}") long reloadCheckMs) {
        this(resolveManagedWordFile(configuredFilePath, uploadPath), reloadCheckMs, true);
    }

    SensitiveFilterService(Path managedWordFile, long reloadCheckMs, boolean hotReloadEnabled) {
        this.managedWordFile = managedWordFile.toAbsolutePath().normalize();
        this.reloadCheckMs = reloadCheckMs;
        this.hotReloadEnabled = hotReloadEnabled;
    }

    @PostConstruct
    public void init() {
        synchronized (mutationMonitor) {
            try {
                bootstrapManagedWordFile();
                reloadFromFileLocked("startup");
            } catch (Exception e) {
                log.error("敏感词字典初始化失败", e);
            }
        }
    }

    @Scheduled(fixedDelayString = "${sensitive.filter.reload-check-ms:5000}")
    public void reloadIfSourceChanged() {
        if (!hotReloadEnabled || reloadCheckMs <= 0) {
            return;
        }
        synchronized (mutationMonitor) {
            try {
                if (!Files.exists(managedWordFile)) {
                    bootstrapManagedWordFile();
                    reloadFromFileLocked("source_missing_bootstrap");
                    return;
                }
                FileStamp currentStamp = readFileStamp(managedWordFile);
                DictionarySnapshot snapshot = snapshotRef.get();
                if (!snapshot.fileStamp.sameFile(currentStamp)) {
                    reloadFromFileLocked("file_change_detected");
                }
            } catch (Exception e) {
                log.warn("敏感词热更新检查失败: file={}", managedWordFile, e);
            }
        }
    }

    public String filter(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return filterWithSnapshot(text, snapshotRef.get());
    }

    public Map<String, Object> listWords(String keyword, Integer page, Integer size) {
        DictionarySnapshot snapshot = snapshotRef.get();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        List<String> filtered = new ArrayList<>();
        for (String word : snapshot.words) {
            if (normalizedKeyword.isEmpty() || word.toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                filtered.add(word);
            }
        }

        int fromIndex = Math.min((safePage - 1) * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());

        Map<String, Object> result = new HashMap<>(snapshot.toStatsMap());
        result.put("list", filtered.subList(fromIndex, toIndex));
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", filtered.size());
        result.put("keyword", keyword == null ? "" : keyword.trim());
        return result;
    }

    public Map<String, Object> getDictionaryStats() {
        return new HashMap<>(snapshotRef.get().toStatsMap());
    }

    public Map<String, Object> addWords(Collection<String> rawWords) {
        synchronized (mutationMonitor) {
            LinkedHashSet<String> current = new LinkedHashSet<>(snapshotRef.get().words);
            List<String> normalizedWords = normalizeWords(rawWords);
            int added = 0;
            for (String word : normalizedWords) {
                if (current.add(word)) {
                    added++;
                }
            }
            if (added == 0) {
                return mutationResult("ADD_SKIPPED", 0, normalizedWords.size(), snapshotRef.get());
            }
            persistWordsLocked(current);
            DictionarySnapshot snapshot = reloadFromFileLocked("admin_add_words");
            return mutationResult("ADD_SUCCESS", added, normalizedWords.size() - added, snapshot);
        }
    }

    public Map<String, Object> removeWords(Collection<String> rawWords) {
        synchronized (mutationMonitor) {
            LinkedHashSet<String> current = new LinkedHashSet<>(snapshotRef.get().words);
            List<String> normalizedWords = normalizeWords(rawWords);
            int removed = 0;
            for (String word : normalizedWords) {
                if (current.remove(word)) {
                    removed++;
                }
            }
            if (removed == 0) {
                return mutationResult("REMOVE_SKIPPED", 0, normalizedWords.size(), snapshotRef.get());
            }
            persistWordsLocked(current);
            DictionarySnapshot snapshot = reloadFromFileLocked("admin_remove_words");
            return mutationResult("REMOVE_SUCCESS", removed, normalizedWords.size() - removed, snapshot);
        }
    }

    public Map<String, Object> replaceWord(String oldWord, String newWord) {
        synchronized (mutationMonitor) {
            String normalizedOld = normalizeWord(oldWord);
            String normalizedNew = normalizeWord(newWord);
            if (normalizedOld == null) {
                throw new IllegalArgumentException("旧词条不能为空");
            }
            if (normalizedNew == null) {
                throw new IllegalArgumentException("新词条不能为空");
            }
            if (normalizedOld.equals(normalizedNew)) {
                Map<String, Object> result = new HashMap<>(snapshotRef.get().toStatsMap());
                result.put("status", "REPLACE_SKIPPED");
                result.put("oldWord", normalizedOld);
                result.put("newWord", normalizedNew);
                return result;
            }
            LinkedHashSet<String> current = new LinkedHashSet<>(snapshotRef.get().words);
            if (!current.remove(normalizedOld)) {
                throw new IllegalArgumentException("旧词条不存在: " + normalizedOld);
            }
            if (current.contains(normalizedNew)) {
                throw new IllegalArgumentException("新词条已存在: " + normalizedNew);
            }
            current.add(normalizedNew);
            persistWordsLocked(current);
            DictionarySnapshot snapshot = reloadFromFileLocked("admin_replace_word");
            Map<String, Object> result = mutationResult("REPLACE_SUCCESS", 1, 0, snapshot);
            result.put("oldWord", normalizedOld);
            result.put("newWord", normalizedNew);
            return result;
        }
    }

    public Map<String, Object> reloadNow() {
        synchronized (mutationMonitor) {
            DictionarySnapshot snapshot = reloadFromFileLocked("manual_reload");
            Map<String, Object> result = new HashMap<>(snapshot.toStatsMap());
            result.put("status", "RELOAD_SUCCESS");
            return result;
        }
    }

    public Map<String, Object> benchmark(int wordCount, int textCount, int textLength, int iterations) {
        int safeWordCount = Math.max(1000, wordCount);
        int safeTextCount = Math.max(100, textCount);
        int safeTextLength = Math.max(32, textLength);
        int safeIterations = Math.max(1, iterations);

        List<String> generatedWords = new ArrayList<>(safeWordCount);
        for (int i = 0; i < safeWordCount; i++) {
            generatedWords.add("敏感词" + String.format(Locale.ROOT, "%07d", i));
        }

        long buildStart = System.nanoTime();
        DictionarySnapshot benchmarkSnapshot = buildSnapshot(generatedWords, "benchmark", FileStamp.memory(), versionCounter.incrementAndGet());
        long buildNanos = System.nanoTime() - buildStart;

        List<String> texts = new ArrayList<>(safeTextCount);
        int replacementsExpectedPerIteration = 0;
        for (int i = 0; i < safeTextCount; i++) {
            if (i % 2 == 0) {
                String keyword = generatedWords.get(i % generatedWords.size());
                texts.add(buildBenchmarkText(keyword, safeTextLength));
                replacementsExpectedPerIteration++;
            } else {
                texts.add(buildBenchmarkText("普通文本" + i, safeTextLength));
            }
        }

        long totalCharacters = 0L;
        long replacedTexts = 0L;
        long filterStart = System.nanoTime();
        for (int iteration = 0; iteration < safeIterations; iteration++) {
            for (String text : texts) {
                String filtered = filterWithSnapshot(text, benchmarkSnapshot);
                totalCharacters += text.length();
                if (!Objects.equals(text, filtered)) {
                    replacedTexts++;
                }
            }
        }
        long filterNanos = System.nanoTime() - filterStart;

        double buildMillis = nanosToMillis(buildNanos);
        double filterMillis = nanosToMillis(filterNanos);
        double charsPerSecond = filterNanos == 0 ? 0D : (totalCharacters * 1_000_000_000D) / filterNanos;
        double textsPerSecond = filterNanos == 0 ? 0D : ((long) safeTextCount * safeIterations * 1_000_000_000D) / filterNanos;

        Map<String, Object> result = new HashMap<>();
        result.put("status", "BENCHMARK_SUCCESS");
        result.put("wordCount", safeWordCount);
        result.put("textCount", safeTextCount);
        result.put("textLength", safeTextLength);
        result.put("iterations", safeIterations);
        result.put("expectedReplacements", (long) replacementsExpectedPerIteration * safeIterations);
        result.put("actualReplacements", replacedTexts);
        result.put("buildMillis", buildMillis);
        result.put("filterMillis", filterMillis);
        result.put("totalCharacters", totalCharacters);
        result.put("throughputCharsPerSecond", roundTo2(charsPerSecond));
        result.put("throughputTextsPerSecond", roundTo2(textsPerSecond));
        result.put("nodeCount", benchmarkSnapshot.nodeCount);
        result.put("maxWordLength", benchmarkSnapshot.maxWordLength);
        return result;
    }

    private String filterWithSnapshot(String text, DictionarySnapshot snapshot) {
        if (snapshot.words.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        TrieNode tempNode = snapshot.root;
        int begin = 0;
        int position = 0;
        while (position < text.length()) {
            char c = text.charAt(position);
            if (isSymbol(c)) {
                if (tempNode == snapshot.root) {
                    result.append(c);
                    begin++;
                }
                position++;
                continue;
            }

            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                result.append(text.charAt(begin));
                position = ++begin;
                tempNode = snapshot.root;
            } else if (tempNode.isKeywordEnd()) {
                result.append(REPLACEMENT);
                begin = ++position;
                tempNode = snapshot.root;
            } else {
                position++;
            }
        }
        result.append(text.substring(begin));
        return result.toString();
    }

    private DictionarySnapshot reloadFromFileLocked(String reason) {
        try {
            List<String> words = readWords(managedWordFile);
            DictionarySnapshot snapshot = buildSnapshot(words, managedWordFile.toString(), readFileStamp(managedWordFile), versionCounter.incrementAndGet());
            snapshotRef.set(snapshot);
            log.info(
                    "敏感词字典刷新完成，reason={}, file={}, version={}, count={}, nodes={}, maxWordLength={}",
                    reason,
                    managedWordFile,
                    snapshot.version,
                    snapshot.words.size(),
                    snapshot.nodeCount,
                    snapshot.maxWordLength
            );
            return snapshot;
        } catch (Exception e) {
            throw new IllegalStateException("敏感词字典刷新失败", e);
        }
    }

    private DictionarySnapshot buildSnapshot(Collection<String> rawWords, String source, FileStamp fileStamp, long version) {
        LinkedHashSet<String> uniqueWords = new LinkedHashSet<>(normalizeWords(rawWords));
        TrieNode root = new TrieNode();
        int nodeCount = 1;
        int maxWordLength = 0;
        for (String word : uniqueWords) {
            maxWordLength = Math.max(maxWordLength, word.length());
            nodeCount += addKeyword(root, word);
        }
        return new DictionarySnapshot(
                root,
                Collections.unmodifiableSet(uniqueWords),
                Instant.now(),
                source,
                fileStamp,
                version,
                nodeCount,
                maxWordLength,
                checksum(uniqueWords)
        );
    }

    private int addKeyword(TrieNode root, String keyword) {
        TrieNode temp = root;
        int created = 0;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode node = temp.getSubNode(c);
            if (node == null) {
                node = new TrieNode();
                temp.addSubNode(c, node);
                created++;
            }
            temp = node;
            if (i == keyword.length() - 1) {
                temp.setKeywordEnd(true);
            }
        }
        return created;
    }

    private void bootstrapManagedWordFile() throws IOException {
        if (Files.exists(managedWordFile)) {
            return;
        }
        Path parent = managedWordFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<String> seedWords = loadSeedWords();
        writeWords(managedWordFile, seedWords);
        log.info("敏感词字典已初始化到外部文件: file={}, count={}", managedWordFile, seedWords.size());
    }

    private List<String> loadSeedWords() throws IOException {
        ClassPathResource resource = new ClassPathResource("sensitive-words.txt");
        if (!resource.exists()) {
            log.warn("敏感词种子文件不存在: classpath:sensitive-words.txt");
            return List.of();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> words = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = normalizeWord(line);
                if (normalized != null && !normalized.startsWith("#")) {
                    words.add(normalized);
                }
            }
            return words;
        }
    }

    private List<String> readWords(Path file) throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        LinkedHashSet<String> words = new LinkedHashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = normalizeWord(line);
                if (normalized == null || normalized.startsWith("#")) {
                    continue;
                }
                words.add(normalized);
            }
        }
        return new ArrayList<>(words);
    }

    private void persistWordsLocked(Set<String> words) {
        try {
            writeWords(managedWordFile, words);
        } catch (IOException e) {
            throw new IllegalStateException("敏感词词库持久化失败: " + managedWordFile, e);
        }
    }

    private void writeWords(Path targetFile, Collection<String> words) throws IOException {
        Path parent = targetFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
        List<String> lines = new ArrayList<>();
        lines.add("# 敏感词词库（由系统维护，支持热更新）");
        lines.addAll(normalizeWords(words));
        Files.write(tempFile, lines, StandardCharsets.UTF_8);
        try {
            Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<String> normalizeWords(Collection<String> rawWords) {
        if (rawWords == null || rawWords.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String rawWord : rawWords) {
            String word = normalizeWord(rawWord);
            if (word != null && !word.startsWith("#")) {
                normalized.add(word);
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String normalizeWord(String rawWord) {
        if (rawWord == null) {
            return null;
        }
        String word = rawWord.trim();
        return word.isEmpty() ? null : word;
    }

    private Map<String, Object> mutationResult(String status, int affected, int ignored, DictionarySnapshot snapshot) {
        Map<String, Object> result = new HashMap<>(snapshot.toStatsMap());
        result.put("status", status);
        result.put("affected", affected);
        result.put("ignored", ignored);
        return result;
    }

    private static Path resolveManagedFilePath(Path configuredPath) {
        return configuredPath.toAbsolutePath().normalize();
    }

    private static Path resolveManagedWordFile(String configuredFilePath, String uploadPath) {
        if (configuredFilePath != null && !configuredFilePath.trim().isEmpty()) {
            return resolveManagedFilePath(Path.of(configuredFilePath.trim()));
        }
        String safeUploadPath = uploadPath == null || uploadPath.trim().isEmpty() ? "uploads" : uploadPath.trim();
        return resolveManagedFilePath(Path.of(safeUploadPath, "sensitive-words.txt"));
    }

    private static FileStamp readFileStamp(Path file) throws IOException {
        return new FileStamp(
                file.toAbsolutePath().normalize().toString(),
                Files.size(file),
                Files.getLastModifiedTime(file).toMillis()
        );
    }

    private static boolean isSymbol(char c) {
        return !Character.isLetterOrDigit(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    private static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static long checksum(Collection<String> words) {
        CRC32 crc32 = new CRC32();
        for (String word : words) {
            byte[] bytes = word.getBytes(StandardCharsets.UTF_8);
            crc32.update(bytes, 0, bytes.length);
            crc32.update('\n');
        }
        return crc32.getValue();
    }

    private static String buildBenchmarkText(String token, int length) {
        StringBuilder builder = new StringBuilder(length + token.length() + 16);
        builder.append("前缀文本");
        builder.append(token);
        while (builder.length() < length) {
            builder.append("填充内容");
        }
        builder.append("结尾");
        return builder.toString();
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000D;
    }

    private static double roundTo2(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private static final class DictionarySnapshot {
        private final TrieNode root;
        private final Set<String> words;
        private final Instant loadedAt;
        private final String source;
        private final FileStamp fileStamp;
        private final long version;
        private final int nodeCount;
        private final int maxWordLength;
        private final long checksum;

        private DictionarySnapshot(
                TrieNode root,
                Set<String> words,
                Instant loadedAt,
                String source,
                FileStamp fileStamp,
                long version,
                int nodeCount,
                int maxWordLength,
                long checksum) {
            this.root = root;
            this.words = words;
            this.loadedAt = loadedAt;
            this.source = source;
            this.fileStamp = fileStamp;
            this.version = version;
            this.nodeCount = nodeCount;
            this.maxWordLength = maxWordLength;
            this.checksum = checksum;
        }

        private static DictionarySnapshot empty() {
            return new DictionarySnapshot(
                    new TrieNode(),
                    Collections.emptySet(),
                    Instant.EPOCH,
                    "uninitialized",
                    FileStamp.memory(),
                    0L,
                    1,
                    0,
                    0L
            );
        }

        private Map<String, Object> toStatsMap() {
            Map<String, Object> result = new HashMap<>();
            result.put("managedFilePath", fileStamp.path);
            result.put("wordCount", words.size());
            result.put("nodeCount", nodeCount);
            result.put("maxWordLength", maxWordLength);
            result.put("loadedAt", loadedAt.toString());
            result.put("version", version);
            result.put("checksum", checksum);
            result.put("source", source);
            result.put("lastModifiedMillis", fileStamp.lastModifiedMillis);
            return result;
        }
    }

    private static final class FileStamp {
        private final String path;
        private final long size;
        private final long lastModifiedMillis;

        private FileStamp(String path, long size, long lastModifiedMillis) {
            this.path = path;
            this.size = size;
            this.lastModifiedMillis = lastModifiedMillis;
        }

        private static FileStamp memory() {
            return new FileStamp("memory", -1L, -1L);
        }

        private boolean sameFile(FileStamp other) {
            return Objects.equals(path, other.path)
                    && size == other.size
                    && lastModifiedMillis == other.lastModifiedMillis;
        }
    }

    private static final class TrieNode {
        private boolean keywordEnd;
        private final Map<Character, TrieNode> subNodes = new HashMap<>();

        private boolean isKeywordEnd() {
            return keywordEnd;
        }

        private void setKeywordEnd(boolean keywordEnd) {
            this.keywordEnd = keywordEnd;
        }

        private void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        private TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }
    }
}
