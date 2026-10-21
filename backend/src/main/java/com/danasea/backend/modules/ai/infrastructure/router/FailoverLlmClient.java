package com.danasea.backend.modules.ai.infrastructure.router;

import com.danasea.backend.modules.ai.application.ports.LlmClientPort;
import com.danasea.backend.modules.ai.domain.models.AiMessage;
import com.danasea.backend.modules.ai.domain.models.LlmResponse;
import com.danasea.backend.modules.ai.infrastructure.cliproxy.CliProxyLlmClient;
import com.danasea.backend.modules.ai.infrastructure.cliproxy.configs.CliProxyProperties;
import com.danasea.backend.modules.ai.infrastructure.groq.GroqLlmClient;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Primary
@Component
public class FailoverLlmClient implements LlmClientPort {

    private final CliProxyLlmClient cliProxyLlmClient;
    private final GroqLlmClient groqLlmClient;
    private final CliProxyProperties cliProxyProperties;

    @Autowired
    public FailoverLlmClient(
            CliProxyLlmClient cliProxyLlmClient,
            GroqLlmClient groqLlmClient,
            CliProxyProperties cliProxyProperties
    ) {
        this.cliProxyLlmClient = cliProxyLlmClient;
        this.groqLlmClient = groqLlmClient;
        this.cliProxyProperties = cliProxyProperties;
    }

    @Override
    public LlmResponse generateResponse(List<AiMessage> messages) {
        return generateResponse(messages, SupportedLanguage.VI);
    }

    @Override
    public LlmResponse generateResponse(List<AiMessage> messages, SupportedLanguage language) {
        if (cliProxyProperties != null && cliProxyProperties.enabled()) {
            try {
                LlmResponse response = cliProxyLlmClient.generateResponse(messages, language);
                if (response != null && (response.getContent() != null || (response.getToolCalls() != null && !response.getToolCalls().isEmpty()))) {
                    return response;
                }
                log.warn("CLI Proxy returned empty content without tool calls. Falling back to Groq.");
            } catch (Exception ex) {
                log.warn("CLI Proxy tier failed completely ({}). Executing ultimate fallback to Groq...", ex.getMessage());
            }
        } else {
            log.info("CLI Proxy is disabled via configuration; routing directly to Groq.");
        }

        return groqLlmClient.generateResponse(messages, language);
    }
}
