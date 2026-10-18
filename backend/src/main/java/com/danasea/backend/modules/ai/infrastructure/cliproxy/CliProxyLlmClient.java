package com.danasea.backend.modules.ai.infrastructure.cliproxy;

import com.danasea.backend.modules.ai.application.ports.LlmClientPort;
import com.danasea.backend.modules.ai.domain.models.AiMessage;
import com.danasea.backend.modules.ai.domain.models.LlmResponse;
import com.danasea.backend.modules.ai.domain.models.ToolCall;
import com.danasea.backend.modules.ai.domain.services.AIToolRegistry;
import com.danasea.backend.modules.ai.domain.services.SystemPromptBuilder;
import com.danasea.backend.modules.ai.infrastructure.cliproxy.configs.CliProxyProperties;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CliProxyLlmClient implements LlmClientPort {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(15);

    private final RestClient restClient;
    private final AIToolRegistry aiToolRegistry;
    private final String apiKey;
    private final String defaultModel;
    private final String fallbackModel;

    private SystemPromptBuilder systemPromptBuilder = new SystemPromptBuilder();
    private LocalizedMessageService localizedMessages = LocalizedMessageService.standalone();

    @Autowired
    void configureI18n(SystemPromptBuilder systemPromptBuilder, LocalizedMessageService messages) {
        this.systemPromptBuilder = systemPromptBuilder;
        this.localizedMessages = messages;
    }

    @Autowired
    public CliProxyLlmClient(
            RestClient.Builder restClientBuilder,
            AIToolRegistry aiToolRegistry,
            CliProxyProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(DEFAULT_READ_TIMEOUT);

        String baseUrl = properties != null ? properties.sanitizedBaseUrl() : "";
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl != null ? baseUrl : "")
                .build();
        this.aiToolRegistry = aiToolRegistry;
        this.apiKey = properties != null ? properties.apiKey() : null;
        this.defaultModel = properties != null ? properties.model() : null;
        this.fallbackModel = properties != null ? properties.fallbackModel() : null;
    }

    public CliProxyLlmClient(
            RestClient restClient,
            AIToolRegistry aiToolRegistry,
            String apiKey,
            String defaultModel,
            String fallbackModel
    ) {
        this.restClient = restClient;
        this.aiToolRegistry = aiToolRegistry;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.fallbackModel = fallbackModel;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    @Override
    public LlmResponse generateResponse(List<AiMessage> messages) {
        return generateResponse(messages, SupportedLanguage.VI);
    }

    @Override
    public LlmResponse generateResponse(List<AiMessage> messages, SupportedLanguage language) {
        SupportedLanguage promptLanguage = language == null ? SupportedLanguage.VI : language;
        try {
            log.info("Attempting primary CLI Proxy model: {}", defaultModel);
            return callModel(messages, defaultModel, promptLanguage);
        } catch (Exception ex) {
            log.warn("CLI Proxy primary model {} failed ({}). Attempting secondary model {}",
                    defaultModel, ex.getMessage(), fallbackModel);
            try {
                return callModel(messages, fallbackModel, promptLanguage);
            } catch (Exception fallbackEx) {
                log.error("CLI Proxy secondary model {} also failed: {}", fallbackModel, fallbackEx.getMessage());
                throw new CliProxyException(
                        "Both models on CLI Proxy failed. Primary [" + defaultModel + "]: " + ex.getMessage()
                                + ", Secondary [" + fallbackModel + "]: " + fallbackEx.getMessage(),
                        fallbackEx
                );
            }
        }
    }

    private LlmResponse callModel(List<AiMessage> messages, String model, SupportedLanguage language) {
        if (model == null || model.isBlank()) {
            throw new CliProxyException("CLI Proxy model is not configured");
        }
        List<CliProxyDto.Message> chatMessages = new ArrayList<>();
        chatMessages.add(new CliProxyDto.Message("system", systemPromptBuilder.buildBasePrompt(language)));

        if (messages != null) {
            for (AiMessage m : messages) {
                String role = m.getRole() != null ? m.getRole().name().toLowerCase() : "user";
                String content = m.getContent();

                if ("assistant".equals(role) && m.getToolCalls() != null) {
                    try {
                        List<ToolCall> tcs = OBJECT_MAPPER.readValue(m.getToolCalls(), new TypeReference<>() {});
                        List<CliProxyDto.ToolCallDto> dtos = tcs.stream()
                                .map(tc -> new CliProxyDto.ToolCallDto(
                                        tc.getId(),
                                        "function",
                                        new CliProxyDto.FunctionDto(tc.getName(), tc.getArguments())
                                ))
                                .toList();
                        chatMessages.add(new CliProxyDto.Message(role, content, dtos, null));
                    } catch (Exception e) {
                        chatMessages.add(new CliProxyDto.Message(role, content));
                    }
                } else if ("tool".equals(role)) {
                    chatMessages.add(new CliProxyDto.Message(role, content, null, m.getToolCalls()));
                } else {
                    chatMessages.add(new CliProxyDto.Message(role, content));
                }
            }
        }

        List<Map<String, Object>> tools = (aiToolRegistry != null && aiToolRegistry.getToolDefinitions() != null)
                ? aiToolRegistry.getToolDefinitions().stream()
                        .map(t -> Map.of("type", "function", "function", t))
                        .toList()
                : List.of();

        CliProxyDto.ChatRequest request = new CliProxyDto.ChatRequest(
                model,
                chatMessages,
                tools.isEmpty() ? null : tools
        );

        try {
            CliProxyDto.ChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(CliProxyDto.ChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new CliProxyException("CLI Proxy returned empty choices for model " + model);
            }

            CliProxyDto.Message msg = response.choices().get(0).message();
            LlmResponse llmResponse = new LlmResponse();
            llmResponse.setContent(msg.content());
            if (apiKey != null && apiKey.length() >= 4) {
                llmResponse.setKeyMasked("..." + apiKey.substring(apiKey.length() - 4));
            } else {
                llmResponse.setKeyMasked("cliproxy");
            }

            if (msg.tool_calls() != null && !msg.tool_calls().isEmpty()) {
                List<ToolCall> toolCalls = msg.tool_calls().stream()
                        .map(tc -> new ToolCall(tc.id(), tc.function().name(), tc.function().arguments()))
                        .toList();
                llmResponse.setToolCalls(toolCalls);
            }

            return llmResponse;
        } catch (ResourceAccessException ex) {
            log.warn("CLI Proxy request timed out (15s) for model {}: {}", model, ex.getMessage());
            throw new CliProxyException("CLI Proxy timeout for model " + model + ": " + ex.getMessage(), ex);
        } catch (RestClientResponseException ex) {
            log.warn("CLI Proxy API returned HTTP {} for model {}: {}", ex.getStatusCode().value(), model, ex.getMessage());
            throw new CliProxyException("CLI Proxy HTTP " + ex.getStatusCode().value() + " for model " + model, ex);
        }
    }
}
