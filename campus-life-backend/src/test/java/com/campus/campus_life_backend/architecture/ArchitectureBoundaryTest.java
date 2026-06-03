package com.campus.campus_life_backend.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBoundaryTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Path MODULES = MAIN_JAVA.resolve("com/campus/campus_life_backend/modules");

    @Test
    void controllersMustNotImportMappers() throws IOException {
        List<String> violations;
        try (Stream<Path> files = Files.walk(MODULES)) {
            violations = files
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .flatMap(this::mapperImportViolations)
                    .toList();
        }

        assertTrue(violations.isEmpty(),
                "Controllers must depend on services/facades, not mappers:\n" + String.join("\n", violations));
    }

    @Test
    void adminServicesMustNotImportCrossDomainMappers() throws IOException {
        List<String> violations;
        try (Stream<Path> files = Files.walk(MODULES.resolve("admin/service"))) {
            violations = files
                    .filter(path -> path.getFileName().toString().endsWith("Service.java"))
                    .flatMap(source -> {
                        try {
                            return Files.readAllLines(source).stream()
                                    .filter(line -> line.startsWith("import "))
                                    .filter(line -> line.contains(".modules."))
                                    .filter(line -> line.contains(".mapper."))
                                    .filter(line -> !line.contains(".modules.admin.mapper.AdminOperationLogMapper"))
                                    .map(line -> source + ": " + line);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        }

        assertTrue(violations.isEmpty(),
                "Admin services may keep admin operation-log mapper only; cross-domain mappers must stay behind facades:\n"
                        + String.join("\n", violations));
    }

    @Test
    void mainBackendMustNotContainAiRuntimeServices() throws IOException {
        Path aiPackage = MAIN_JAVA.resolve("com/campus/campus_life_backend/ai");
        if (!Files.exists(aiPackage)) {
            return;
        }

        List<String> violations;
        try (Stream<Path> files = Files.walk(aiPackage)) {
            violations = files
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .flatMap(this::aiRuntimeBeanViolations)
                    .toList();
        }

        assertTrue(violations.isEmpty(),
                "AI runtime belongs to campus-life-ai; main backend may keep only deprecated compatibility endpoints/DTOs:\n"
                        + String.join("\n", violations));
    }

    private Stream<String> mapperImportViolations(Path source) {
        try {
            return Files.readAllLines(source).stream()
                    .filter(line -> line.startsWith("import "))
                    .filter(line -> line.contains(".mapper."))
                    .map(line -> source + ": " + line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<String> aiRuntimeBeanViolations(Path source) {
        try {
            return Files.readAllLines(source).stream()
                    .filter(line -> line.contains("@Service") || line.contains("@Component"))
                    .map(line -> source + ": " + line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
