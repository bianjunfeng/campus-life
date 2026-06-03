package com.campus.campus_life_backend.modules.forum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveFilterServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBootstrapSeedWordsAndFilterKnownContent() throws Exception {
        SensitiveFilterService service = new SensitiveFilterService(tempDir.resolve("sensitive-words.txt"), 5000L, true);

        service.init();

        assertTrue(Files.exists(tempDir.resolve("sensitive-words.txt")));
        assertEquals("这是***内容", service.filter("这是赌博内容"));

        Map<String, Object> stats = service.getDictionaryStats();
        assertTrue(((Number) stats.get("wordCount")).intValue() >= 8);
        assertEquals(tempDir.resolve("sensitive-words.txt").toAbsolutePath().normalize().toString(), stats.get("managedFilePath"));
    }

    @Test
    void shouldPersistMutationsAndReloadFromExternalChanges() throws Exception {
        Path managedFile = tempDir.resolve("managed-sensitive-words.txt");
        SensitiveFilterService service = new SensitiveFilterService(managedFile, 5000L, true);
        service.init();

        Map<String, Object> addResult = service.addWords(List.of("违禁词", "测试违禁"));
        assertEquals("ADD_SUCCESS", addResult.get("status"));
        assertEquals("这是***样例", service.filter("这是违禁词样例"));

        Map<String, Object> replaceResult = service.replaceWord("违禁词", "违规词");
        assertEquals("REPLACE_SUCCESS", replaceResult.get("status"));
        assertEquals("这是违禁词样例", service.filter("这是违禁词样例"));
        assertEquals("这是***样例", service.filter("这是违规词样例"));

        Map<String, Object> removeResult = service.removeWords(List.of("测试违禁"));
        assertEquals("REMOVE_SUCCESS", removeResult.get("status"));
        assertEquals("这里有测试违禁内容", service.filter("这里有测试违禁内容"));

        Files.writeString(
                managedFile,
                "# external edit\n外部热更词\n",
                StandardCharsets.UTF_8
        );
        service.reloadIfSourceChanged();

        assertEquals("这是***", service.filter("这是外部热更词"));
        Map<String, Object> listed = service.listWords("外部", 1, 20);
        assertEquals(1, ((List<?>) listed.get("list")).size());
        assertEquals(1, ((Number) listed.get("total")).intValue());
    }

    @Test
    void shouldProvideStableBenchmarkMetrics() {
        SensitiveFilterService service = new SensitiveFilterService(tempDir.resolve("benchmark-sensitive-words.txt"), 5000L, true);
        service.init();

        Map<String, Object> metrics = service.benchmark(5000, 200, 64, 3);

        assertEquals("BENCHMARK_SUCCESS", metrics.get("status"));
        assertEquals(metrics.get("expectedReplacements"), metrics.get("actualReplacements"));
        assertTrue(((Number) metrics.get("throughputCharsPerSecond")).doubleValue() > 0D);
        assertTrue(((Number) metrics.get("nodeCount")).intValue() > 0);
        assertNotNull(metrics.get("buildMillis"));
        assertNotNull(metrics.get("filterMillis"));
        assertFalse(((Number) metrics.get("totalCharacters")).longValue() <= 0L);
    }
}
