package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.application.ports.ConfirmationCardStorePort;
import com.danasea.backend.modules.ai.application.ports.LlmClientPort;
import com.danasea.backend.modules.ai.application.ports.ModerationPort;
import com.danasea.backend.modules.ai.application.usecases.ChatUseCase;
import com.danasea.backend.modules.ai.application.usecases.ConfirmBookingUseCase;
import com.danasea.backend.modules.ai.domain.TrustTier;
import com.danasea.backend.modules.ai.domain.models.AiMessageRole;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.danasea.backend.modules.ai.domain.models.LlmResponse;
import com.danasea.backend.modules.ai.domain.services.AssistantAuditLogService;
import com.danasea.backend.modules.ai.domain.services.ChatHistoryService;
import com.danasea.backend.modules.ai.infrastructure.redis.RedisRateLimiter;
import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.application.usecases.GetPublicServiceDetailUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Empirical Adversarial & Stress Test Harness for DANASEA AI Assistant Components.
 * Designed by Challenger 1 (Generation 2) to probe failure modes, concurrency race conditions,
 * boundary vulnerabilities, and security evasions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI Assistant Empirical Adversarial & Stress Test Suite")
public class AiAssistantAdversarialStressTest {

    // =========================================================================
    // 1. RATE LIMITER ADVERSARIAL STRESS SUITE
    // =========================================================================
    @Nested
    @DisplayName("1. Rate Limiter Adversarial Stress Tests")
    class RateLimiterTests {

        @Mock
        private LettuceBasedProxyManager<byte[]> proxyManager;

        @Mock
        private RemoteBucketBuilder<byte[]> proxyManagerBuilder;

        @Mock
        private BucketProxy ipBucket;

        @Mock
        private BucketProxy userBucket;

        @Mock
        private BucketProxy anonBucket;

        private RedisRateLimiter rateLimiter;

        @BeforeEach
        void setUp() {
            lenient().when(proxyManager.builder()).thenReturn(proxyManagerBuilder);
            rateLimiter = new RedisRateLimiter(proxyManager);
        }

        @Test
        @DisplayName("ADV-RL-1: Burst Load - 100 requests exhaust IP bucket, triggering soft-throttle degradation for subsequent requests")
        void testBurstLoad_IpExhaustionTriggersDegradation() {
            when(proxyManagerBuilder.build(argThat(k -> k != null && new String(k).startsWith("ai:ratelimit:ip:")), any(BucketConfiguration.class)))
                    .thenReturn(ipBucket);
            when(proxyManagerBuilder.build(argThat(k -> k != null && new String(k).startsWith("ai:ratelimit:user:")), any(BucketConfiguration.class)))
                    .thenReturn(userBucket);

            // Simulate burst: first request IP normal (true), second request IP exhausted (false)
            when(ipBucket.tryConsume(1))
                    .thenReturn(true)   // Request 1: IP normal
                    .thenReturn(false);  // Request 2: IP exhausted (anomaly)

            when(userBucket.tryConsume(1)).thenReturn(true);

            // Request 1: normal TRUSTED
            boolean allowed1 = rateLimiter.isAllowed("user-trusted-1", "203.0.113.10", TrustTier.TRUSTED);
            assertTrue(allowed1);

            // Request 2: IP exhausted -> degrades TRUSTED to UNVERIFIED (capacity 5)
            ArgumentCaptor<BucketConfiguration> configCaptor = ArgumentCaptor.forClass(BucketConfiguration.class);
            boolean allowed2 = rateLimiter.isAllowed("user-trusted-1", "203.0.113.10", TrustTier.TRUSTED);
            assertTrue(allowed2);

            verify(proxyManagerBuilder, times(4)).build(any(), configCaptor.capture());
            // In request 2, user capacity was degraded to 5
            BucketConfiguration degradedConfig = configCaptor.getAllValues().get(3);
            assertEquals(5, degradedConfig.getBandwidths()[0].getCapacity());
        }

        @Test
        @DisplayName("ADV-RL-2: Anonymous IP Variation - Guest sessions correctly partitioned by 'ai:ratelimit:anon:{ip}'")
        void testAnonymousUser_PartitionedByIpBucket() {
            when(proxyManagerBuilder.build(any(), any(BucketConfiguration.class)))
                    .thenReturn(ipBucket)
                    .thenReturn(anonBucket);

            when(ipBucket.tryConsume(1)).thenReturn(true);
            when(anonBucket.tryConsume(1)).thenReturn(true);

            ArgumentCaptor<byte[]> keyCaptor = ArgumentCaptor.forClass(byte[].class);

            boolean allowed = rateLimiter.isAllowed(null, "198.51.100.25", TrustTier.UNVERIFIED);
            assertTrue(allowed);

            verify(proxyManagerBuilder, times(2)).build(keyCaptor.capture(), any(BucketConfiguration.class));
            String ipKey = new String(keyCaptor.getAllValues().get(0));
            String anonKey = new String(keyCaptor.getAllValues().get(1));

            assertEquals("ai:ratelimit:ip:198.51.100.25", ipKey);
            assertEquals("ai:ratelimit:anon:198.51.100.25", anonKey);
        }

        @Test
        @DisplayName("ADV-RL-3: Anonymous User Token Exhaustion - 6th request from same IP is blocked (capacity=5)")
        void testAnonymousUser_ExhaustionBlocksRequest() {
            when(proxyManagerBuilder.build(any(), any(BucketConfiguration.class)))
                    .thenReturn(ipBucket)
                    .thenReturn(anonBucket);

            when(ipBucket.tryConsume(1)).thenReturn(true);
            when(anonBucket.tryConsume(1)).thenReturn(false); // Anon bucket exhausted

            boolean allowed = rateLimiter.isAllowed(null, "198.51.100.30", TrustTier.UNVERIFIED);
            assertFalse(allowed, "Anonymous user exceeding 5 req/min must be rejected");
        }

        @Test
        @DisplayName("ADV-RL-4: Rate Limiter Edge Case - Null userId and Null/Blank IP returns false safely")
        void testRateLimiter_NullUserIdAndNullIp_BypassesConsumption() {
            // After remediation: when both userId and ipAddress are null or blank,
            // rate limiter rejects the request (returns false) to prevent bypass.
            boolean allowedNullIp = rateLimiter.isAllowed(null, null, TrustTier.UNVERIFIED);
            boolean allowedBlankIp = rateLimiter.isAllowed(null, "   ", TrustTier.UNVERIFIED);

            assertFalse(allowedNullIp, "Edge case: null IP and null user must return false");
            assertFalse(allowedBlankIp, "Edge case: blank IP and null user must return false");
            verifyNoInteractions(proxyManagerBuilder);
        }
    }

    // =========================================================================
    // =========================================================================


    // =========================================================================
    // 3. MODERATION TRUNCATION & BOUNDARY ADVERSARIAL SUITE
    // =========================================================================


    // =========================================================================
    // 4. BOOKING RE-VALIDATION & RETRY TRACKING ADVERSARIAL SUITE
    // =========================================================================
    @Nested
    @DisplayName("4. Booking Confirmation Re-Validation Tests")
    class BookingConfirmationTests {

        @Mock
        private ConfirmationCardStorePort cardStorePort;

        @Mock
        private GetPublicServiceDetailUseCase getServiceDetailUseCase;

        @Mock
        private StringRedisTemplate redisTemplate;

        @Mock
        private ValueOperations<String, String> valueOperations;

        private ConfirmBookingUseCase confirmBookingUseCase;
        private final UUID userId = UUID.randomUUID();
        private final UUID serviceId = UUID.randomUUID();
        private final UUID conversationId = UUID.randomUUID();
        private final String sessionId = "session-test";

        @BeforeEach
        void setUp() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            confirmBookingUseCase = new ConfirmBookingUseCase(cardStorePort, getServiceDetailUseCase, redisTemplate);
        }

        @Test
        @DisplayName("ADV-BC-1: Exhausted Slots Handling - Card cancelled, status 'out_of_stock' without creating new draft card")
        void testExhaustedSlots_CancelsCardAndReturnsOutOfStock() {
            ConfirmationCard pendingCard = ConfirmationCard.builder()
                    .id("card-pending-1")
                    .conversationId(conversationId)
                    .serviceId(serviceId)
                    .price(new BigDecimal("500000.00"))
                    .date("2026-09-20T08:00:00")
                    .quantity(2)
                    .status("PENDING")
                    .retryCount(0)
                    .build();

            ServiceDetailResult emptySlotsResult = ServiceDetailResult.builder()
                    .price(new BigDecimal("500000.00"))
                    .availableSlots(Collections.emptyList()) // 0 slots
                    .build();

            when(cardStorePort.findById("card-pending-1")).thenReturn(Optional.of(pendingCard));
            when(getServiceDetailUseCase.execute(serviceId, userId, sessionId)).thenReturn(emptySlotsResult);

            Map<String, Object> response = confirmBookingUseCase.execute("card-pending-1", userId, sessionId);

            assertEquals("out_of_stock", response.get("status"));
            assertEquals("CANCELLED", pendingCard.getStatus());
            verify(cardStorePort, times(1)).save(pendingCard);
            // Verify no new card created
            assertNull(response.get("newCard"));
        }

        @Test
        @DisplayName("ADV-BC-2: 2-Retry Limit Cap - 3rd attempt with changed price is rejected with error")
        void testRetryLimitCap_RejectsThirdAttempt() {
            ConfirmationCard pendingCard = ConfirmationCard.builder()
                    .id("card-retry-2")
                    .conversationId(conversationId)
                    .serviceId(serviceId)
                    .price(new BigDecimal("500000.00"))
                    .date("2026-09-20T08:00:00")
                    .quantity(2)
                    .status("PENDING")
                    .retryCount(2) // already retried twice
                    .build();

            when(valueOperations.get("ai:booking:retries:" + conversationId)).thenReturn("2");

            ServiceDetailResult changedPriceResult = ServiceDetailResult.builder()
                    .price(new BigDecimal("550000.00")) // price changed again
                    .availableSlots(List.of("2026-09-20T08:00:00"))
                    .build();

            when(cardStorePort.findById("card-retry-2")).thenReturn(Optional.of(pendingCard));
            when(getServiceDetailUseCase.execute(serviceId, userId, sessionId)).thenReturn(changedPriceResult);

            Map<String, Object> response = confirmBookingUseCase.execute("card-retry-2", userId, sessionId);

            assertEquals("error", response.get("status"));
            assertTrue(response.get("message").toString().contains("too many times"));
            assertEquals("CANCELLED", pendingCard.getStatus());
        }

        @Test
        @DisplayName("ADV-BC-3: Reason Attribute Populated - PRICE_CHANGED correctly set on newly generated draft card")
        void testReasonAttribute_PopulatedOnPriceChange() {
            ConfirmationCard pendingCard = ConfirmationCard.builder()
                    .id("card-p1")
                    .conversationId(conversationId)
                    .serviceId(serviceId)
                    .price(new BigDecimal("500000.00"))
                    .date("2026-09-20T08:00:00")
                    .quantity(1)
                    .status("PENDING")
                    .retryCount(0)
                    .build();

            ServiceDetailResult newPriceResult = ServiceDetailResult.builder()
                    .price(new BigDecimal("550000.00"))
                    .availableSlots(List.of("2026-09-20T08:00:00"))
                    .build();

            when(cardStorePort.findById("card-p1")).thenReturn(Optional.of(pendingCard));
            when(getServiceDetailUseCase.execute(serviceId, userId, sessionId)).thenReturn(newPriceResult);

            Map<String, Object> response = confirmBookingUseCase.execute("card-p1", userId, sessionId);

            assertEquals("alternative_needed", response.get("status"));
            assertEquals("PRICE_CHANGED", response.get("reason"));
            ConfirmationCard newCard = (ConfirmationCard) response.get("newCard");
            assertNotNull(newCard);
            assertEquals("PRICE_CHANGED", newCard.getReason());
            assertEquals(1, newCard.getRetryCount());
        }
    }

    // =========================================================================
    // 5. CHAT USE CASE POLICY REJECTION & AUDIT LOGGING ADVERSARIAL SUITE
    // =========================================================================
    @Nested
    @DisplayName("5. Chat Use Case Policy Enforcement & Audit Logging Tests")
    class ChatUseCasePolicyTests {

        @Mock
        private LlmClientPort llmClientPort;

        @Mock
        private ModerationPort moderationPort;

        @Mock
        private ChatHistoryService chatHistoryService;

        @Mock
        private AssistantAuditLogService auditLogService;

        private ChatUseCase chatUseCase;
        private final UUID conversationId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            chatUseCase = new ChatUseCase(
                    llmClientPort,
                    moderationPort,
                    chatHistoryService,
                    Collections.emptyList(),
                    new ObjectMapper(),
                    auditLogService
            );
        }

        @Test
        @DisplayName("ADV-POL-1: Policy Keyword Rejection - LLM mentions 'hoàn tiền' without get_policy call is rejected")
        void testPolicyKeywordRejection_WithoutGetPolicyCall() {
            when(moderationPort.isSafe(anyString())).thenReturn(true);

            // LLM hallucinates refund policy without invoking get_policy tool
            LlmResponse hallucinatedResponse = new LlmResponse();
            hallucinatedResponse.setContent("Chúng tôi sẽ hoàn tiền 100% nếu bạn hủy trước 24 giờ.");
            hallucinatedResponse.setToolCalls(null);

            when(llmClientPort.generateResponse(anyList())).thenReturn(hallucinatedResponse);

            LlmResponse response = chatUseCase.processMessage(conversationId, "Quy định hoàn tiền thế nào?");

            // Must be overridden with standard rejection message
            assertTrue(response.getContent().contains("I cannot provide cancellation or refund policy details"));
            assertTrue(response.getContent().contains("official policy source"));
        }

        @Test
        @DisplayName("ADV-POL-2: Moderation Block Audit Logging - Blocked injection triggers audit log entry")
        void testModerationBlocked_LoggedToAuditLog() {
            when(moderationPort.isSafe("DROP TABLE users;")).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> {
                chatUseCase.processMessage(conversationId, "DROP TABLE users;");
            });

            // Verify auditLogService logged the blocked attempt
            verify(auditLogService).logChatAction(
                    eq(conversationId),
                    isNull(),
                    eq("DROP TABLE users;"),
                    eq("BLOCKED: Prompt injection detected"),
                    isNull()
            );
        }

        @Test
        @DisplayName("ADV-POL-3: Padding Evasion Prevention - User message is capped to 1800 chars before history and LLM")
        void testPaddingEvasion_CappedBeforeHistoryAndLlm() {
            when(moderationPort.isSafe(anyString())).thenReturn(true);
            LlmResponse mockResp = new LlmResponse();
            mockResp.setContent("Response to query");
            when(llmClientPort.generateResponse(anyList())).thenReturn(mockResp);

            String longMsg = "x".repeat(2000);
            chatUseCase.processMessage(conversationId, longMsg);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(chatHistoryService).appendMessage(eq(conversationId), eq(AiMessageRole.USER), captor.capture(), isNull());

            assertEquals(1800, captor.getValue().length(), "User message saved to history must be capped to 1800 chars");
        }
    }
}
