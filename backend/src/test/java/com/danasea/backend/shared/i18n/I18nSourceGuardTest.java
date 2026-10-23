package com.danasea.backend.shared.i18n;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class I18nSourceGuardTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");

    @Test
    void sourceDoesNotReintroduceServletOrResponseMessageLiterals() throws IOException {
        String source = readJavaSources(MAIN_JAVA);

        assertFalse(
                source.contains("sendError("),
                "Use the JSON ErrorResponse contract instead of sendError");
        assertFalse(
                Pattern.compile("new\\s+ErrorResponse\\(\\s*[^,\\r\\n]+,\\s*\\\"")
                        .matcher(source)
                        .find(),
                "ErrorResponse messages must come from message bundles");
        assertFalse(
                Pattern.compile(
                                "new\\s+ErrorResponse\\([^)]*,\\s*(?:ex|exception)\\.getMessage\\(\\)",
                                Pattern.DOTALL)
                        .matcher(source)
                        .find(),
                "Technical exception messages must not be exposed in ErrorResponse");
        assertFalse(
                Pattern.compile(
                                "@(NotNull|NotBlank|NotEmpty|Size|Email|Pattern|Min|Max|Positive|Future|Past)"
                                    + "\\([^)]*message\\s*=\\s*\\\"(?!\\{)",
                                Pattern.DOTALL)
                        .matcher(source)
                        .find(),
                "Validation annotation messages must use {validation.*} keys");
    }

    @Test
    void aiAndNotificationPresentationFallbacksStayLocalized() throws IOException {
        String chat =
                Files.readString(
                        MAIN_JAVA.resolve(
                                "com/danasea/backend/modules/ai/application/usecases/ChatUseCase.java"));
        String gateway =
                Files.readString(
                        MAIN_JAVA.resolve(
                                "com/danasea/backend/modules/ai/infrastructure/cliproxy/CliProxyLlmClient.java"));
        String weatherJob =
                Files.readString(
                        MAIN_JAVA.resolve(
                                "com/danasea/backend/modules/weather/infrastructure/jobs/SlotWeatherMonitoringJob.java"));

        for (String forbidden :
                List.of(
                        "temporarily unavailable",
                        "taking too long",
                        "Unknown tool",
                        "Tôi không thể cung cấp thông tin về chính sách")) {
            assertFalse(
                    chat.contains(forbidden), "Chat fallback must use a message key: " + forbidden);
            assertFalse(
                    gateway.contains(forbidden), "LLM fallback must use a message key: " + forbidden);
        }
        assertFalse(
                Pattern.compile(
                                "sendNotificationUseCase\\.execute\\((?!\\s*new\\s+NotificationCommand)",
                                Pattern.DOTALL)
                        .matcher(weatherJob)
                        .find(),
                "Weather notifications must use NotificationCommand message refs");
    }

    private String readJavaSources(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            StringBuilder source = new StringBuilder();
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                source.append(Files.readString(path)).append('\n');
            }
            return source.toString();
        }
    }
}
