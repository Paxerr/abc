package com.danasea.backend.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.danasea.backend.modules.ai.application.ports.AiExecutionBudget;
import com.danasea.backend.modules.ai.infrastructure.cliproxy.CliProxyLlmClient;
import com.danasea.backend.shared.i18n.SupportedLanguage;

class AssistantRequestDeadlineTest {
    @Test void exhaustedRequestBudgetDoesNotStartAnotherProxyRequest() {
        var transport = mock(RestClient.class);
        var client = new CliProxyLlmClient(transport, null, "gateway-test-key", "default", "fallback");
        try (var budget = AiExecutionBudget.open(Duration.ofMillis(1), 6)) {
            var response = client.generateResponse(List.of(), SupportedLanguage.EN);
            assertThat(response.getResponseStatus()).isEqualTo("PARTIAL");
            assertThat(response.getContent()).contains("taking too long");
        }
        verifyNoInteractions(transport);
        assertThat(AiExecutionBudget.hasTimeFor(Duration.ofSeconds(15))).isTrue();
    }
}
