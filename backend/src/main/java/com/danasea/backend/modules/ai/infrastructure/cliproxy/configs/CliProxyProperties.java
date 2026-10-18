package com.danasea.backend.modules.ai.infrastructure.cliproxy.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "ai.cliproxy")
public record CliProxyProperties(
        boolean enabled,
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotBlank String model,
        @NotBlank String fallbackModel
) {
    public String sanitizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String url = baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.endsWith("/v1")) {
            url += "/v1";
        }
        return url;
    }
}
