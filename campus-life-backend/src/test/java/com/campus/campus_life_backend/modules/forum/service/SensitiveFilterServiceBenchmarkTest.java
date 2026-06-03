package com.campus.campus_life_backend.modules.forum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveFilterServiceBenchmarkTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPrintBenchmarkMetricsWithConfigurableScale() {
        int wordCount = Integer.getInteger("sensitive.filter.benchmark.word-count", 20000);
        int textCount = Integer.getInteger("sensitive.filter.benchmark.text-count", 500);
        int textLength = Integer.getInteger("sensitive.filter.benchmark.text-length", 80);
        int iterations = Integer.getInteger("sensitive.filter.benchmark.iterations", 5);

        SensitiveFilterService service = new SensitiveFilterService(tempDir.resolve("benchmark-words.txt"), 5000L, false);
        service.init();

        Map<String, Object> metrics = service.benchmark(wordCount, textCount, textLength, iterations);
        System.out.println("Sensitive filter benchmark: " + metrics);

        assertEquals("BENCHMARK_SUCCESS", metrics.get("status"));
        assertEquals(metrics.get("expectedReplacements"), metrics.get("actualReplacements"));
        assertTrue(((Number) metrics.get("throughputTextsPerSecond")).doubleValue() > 0D);
    }
}
