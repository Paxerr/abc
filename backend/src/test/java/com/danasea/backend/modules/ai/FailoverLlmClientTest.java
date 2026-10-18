package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.domain.models.AiMessage;
import com.danasea.backend.modules.ai.domain.models.AiMessageRole;
import com.danasea.backend.modules.ai.domain.models.LlmResponse;
import com.danasea.backend.modules.ai.infrastructure.cliproxy.CliProxyException;
import com.danasea.backend.modules.ai.infrastructure.cliproxy.CliProxyLlmClient;
import com.danasea.backend.modules.ai.infrastructure.cliproxy.configs.CliProxyProperties;
import com.danasea.backend.modules.ai.infrastructure.groq.GroqLlmClient;
import com.danasea.backend.modules.ai.infrastructure.router.FailoverLlmClient;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailoverLlmClient 3-Tier Failover Unit Tests")
public class FailoverLlmClientTest {

    @Mock
    private CliProxyLlmClient cliProxyLlmClient;

    @Mock
    private GroqLlmClient groqLlmClient;

    private CliProxyProperties enabledProperties;
    private CliProxyProperties disabledProperties;

    @BeforeEach
    void setUp() {
        enabledProperties = new CliProxyProperties(
                true,
                "http://localhost:8318/v1",
                "sk-cliproxy-test",
                "gemini-3.5-flash",
                "openai/gpt-oss-120b"
        );
        disabledProperties = new CliProxyProperties(
                false,
                "http://localhost:8318/v1",
                "sk-cliproxy-test",
                "gemini-3.5-flash",
                "openai/gpt-oss-120b"
        );
    }

    @Test
    @DisplayName("Tier 1/2 Success: CLI Proxy succeeds, Groq is never touched")
    void testCliProxySuccess_GroqNeverInvoked() {
        FailoverLlmClient client = new FailoverLlmClient(cliProxyLlmClient, groqLlmClient, enabledProperties);

        LlmResponse cliResponse = new LlmResponse();
        cliResponse.setContent("Phản hồi từ Gemini trên CLI Proxy");
        cliResponse.setKeyMasked("cliproxy");

        when(cliProxyLlmClient.generateResponse(any(), eq(SupportedLanguage.VI))).thenReturn(cliResponse);

        AiMessage msg = new AiMessage();
        msg.setRole(AiMessageRole.USER);
        msg.setContent("Xin chào");

        LlmResponse result = client.generateResponse(List.of(msg), SupportedLanguage.VI);

        assertNotNull(result);
        assertEquals("Phản hồi từ Gemini trên CLI Proxy", result.getContent());
        assertEquals("cliproxy", result.getKeyMasked());
        verify(cliProxyLlmClient, times(1)).generateResponse(any(), eq(SupportedLanguage.VI));
        verifyNoInteractions(groqLlmClient);
    }

    @Test
    @DisplayName("Tier 3 Fallback: When CLI Proxy fails, system falls back to Groq")
    void testCliProxyFails_FallsBackToGroq() {
        FailoverLlmClient client = new FailoverLlmClient(cliProxyLlmClient, groqLlmClient, enabledProperties);

        when(cliProxyLlmClient.generateResponse(any(), eq(SupportedLanguage.VI)))
                .thenThrow(new CliProxyException("Connection refused to CLI Proxy"));

        LlmResponse groqResponse = new LlmResponse();
        groqResponse.setContent("Phản hồi từ Groq fallback");
        groqResponse.setKeyMasked("...1234");

        when(groqLlmClient.generateResponse(any(), eq(SupportedLanguage.VI))).thenReturn(groqResponse);

        AiMessage msg = new AiMessage();
        msg.setRole(AiMessageRole.USER);
        msg.setContent("Xin chào");

        LlmResponse result = client.generateResponse(List.of(msg), SupportedLanguage.VI);

        assertNotNull(result);
        assertEquals("Phản hồi từ Groq fallback", result.getContent());
        assertEquals("...1234", result.getKeyMasked());
        verify(cliProxyLlmClient, times(1)).generateResponse(any(), eq(SupportedLanguage.VI));
        verify(groqLlmClient, times(1)).generateResponse(any(), eq(SupportedLanguage.VI));
    }

    @Test
    @DisplayName("CLI Proxy disabled in configuration: routes directly to Groq")
    void testCliProxyDisabled_RoutesDirectlyToGroq() {
        FailoverLlmClient client = new FailoverLlmClient(cliProxyLlmClient, groqLlmClient, disabledProperties);

        LlmResponse groqResponse = new LlmResponse();
        groqResponse.setContent("Phản hồi trực tiếp từ Groq");
        when(groqLlmClient.generateResponse(any(), eq(SupportedLanguage.VI))).thenReturn(groqResponse);

        AiMessage msg = new AiMessage();
        msg.setRole(AiMessageRole.USER);
        msg.setContent("Xin chào");

        LlmResponse result = client.generateResponse(List.of(msg), SupportedLanguage.VI);

        assertNotNull(result);
        assertEquals("Phản hồi trực tiếp từ Groq", result.getContent());
        verifyNoInteractions(cliProxyLlmClient);
        verify(groqLlmClient, times(1)).generateResponse(any(), eq(SupportedLanguage.VI));
    }
}
