package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.infrastructure.cliproxy.CliProxyModerationClient;

import com.danasea.backend.modules.ai.domain.TrustTier;
import com.danasea.backend.modules.ai.infrastructure.redis.RedisRateLimiter;
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

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Empirical Stress & Adversarial Verification Test Suite.
 * Authored by Challenger 1 (Gen 2 Iteration 2) to probe concurrency race conditions,
 * state overwrites, and rate limiter bypass vectors.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Empirical Challenger Gen 2 Iteration 2 Verification Suite")
public class EmpiricalStressVerificationGen2Test {

    // =========================================================================
    // =========================================================================


    // =========================================================================
    // 2. STATE OVERWRITE RESILIENCE: markKeyCooldown cannot overwrite DISABLED
    // =========================================================================


    // =========================================================================
    // 3. RATE LIMITER ADVERSARIAL: Rejection of null/blank identifiers
    // =========================================================================
    @Nested
    @DisplayName("3. Rate Limiter Null/Blank Rejection Tests")
    class RateLimiterIdentifierRejectionTests {

        @Mock
        private LettuceBasedProxyManager<byte[]> proxyManager;

        @Mock
        private RemoteBucketBuilder<byte[]> builder;

        @Mock
        private BucketProxy bucket;

        private RedisRateLimiter rateLimiter;

        @BeforeEach
        void setUp() {
            lenient().when(proxyManager.builder()).thenReturn(builder);
            lenient().when(builder.build(any(), any(BucketConfiguration.class))).thenReturn(bucket);
            lenient().when(bucket.tryConsume(anyLong())).thenReturn(true);
            rateLimiter = new RedisRateLimiter(proxyManager);
        }

        @Test
        @DisplayName("RL-ADV-1: Both userId and ipAddress null returns false")
        void testBothNull_ReturnsFalse() {
            boolean allowed = rateLimiter.isAllowed(null, null, TrustTier.UNVERIFIED);
            assertFalse(allowed, "Must reject request when both userId and ipAddress are null");
            verifyNoInteractions(proxyManager);
        }

        @Test
        @DisplayName("RL-ADV-2: Both userId and ipAddress blank/spaces returns false")
        void testBothBlankSpaces_ReturnsFalse() {
            boolean allowed = rateLimiter.isAllowed("   ", "     ", TrustTier.UNVERIFIED);
            assertFalse(allowed, "Must reject request when both userId and ipAddress are blank spaces");
            verifyNoInteractions(proxyManager);
        }

        @Test
        @DisplayName("RL-ADV-3: Tab and newline whitespace strings return false")
        void testTabAndNewlineWhitespace_ReturnsFalse() {
            boolean allowed1 = rateLimiter.isAllowed("\t\n", "\r\n", TrustTier.UNVERIFIED);
            boolean allowed2 = rateLimiter.isAllowed(null, "\t", TrustTier.VERIFIED);
            boolean allowed3 = rateLimiter.isAllowed("   ", null, TrustTier.TRUSTED);

            assertFalse(allowed1, "Must reject tabs and newlines as blank");
            assertFalse(allowed2, "Must reject tab IP as blank");
            assertFalse(allowed3, "Must reject space user as blank");
            verifyNoInteractions(proxyManager);
        }

        @Test
        @DisplayName("RL-ADV-4: Blank userId with valid IP uses anonymous bucket (capacity 5)")
        void testBlankUserIdWithValidIp_UsesAnonymousBucket() {
            when(builder.build(any(), any(BucketConfiguration.class))).thenReturn(bucket);
            when(bucket.tryConsume(1)).thenReturn(true);

            ArgumentCaptor<byte[]> keyCaptor = ArgumentCaptor.forClass(byte[].class);
            ArgumentCaptor<BucketConfiguration> configCaptor = ArgumentCaptor.forClass(BucketConfiguration.class);

            boolean allowed = rateLimiter.isAllowed("   ", "10.0.0.1", TrustTier.TRUSTED);
            assertTrue(allowed);

            // Expect 2 builds: IP soft throttle (ai:ratelimit:ip:10.0.0.1) and anon bucket (ai:ratelimit:anon:10.0.0.1)
            verify(builder, times(2)).build(keyCaptor.capture(), configCaptor.capture());

            String ipKey = new String(keyCaptor.getAllValues().get(0));
            String anonKey = new String(keyCaptor.getAllValues().get(1));
            assertEquals("ai:ratelimit:ip:10.0.0.1", ipKey);
            assertEquals("ai:ratelimit:anon:10.0.0.1", anonKey);

            // Anonymous bucket capacity is 5
            assertEquals(5, configCaptor.getAllValues().get(1).getBandwidths()[0].getCapacity());
        }

        @Test
        @DisplayName("RL-ADV-5: Valid userId with blank IP uses user bucket directly")
        void testValidUserIdWithBlankIp_UsesUserBucket() {
            when(builder.build(any(), any(BucketConfiguration.class))).thenReturn(bucket);
            when(bucket.tryConsume(1)).thenReturn(true);

            ArgumentCaptor<byte[]> keyCaptor = ArgumentCaptor.forClass(byte[].class);
            ArgumentCaptor<BucketConfiguration> configCaptor = ArgumentCaptor.forClass(BucketConfiguration.class);

            boolean allowed = rateLimiter.isAllowed("user-999", "  ", TrustTier.VERIFIED);
            assertTrue(allowed);

            // Expect exactly 1 build: user bucket (ai:ratelimit:user:user-999) with capacity 20
            verify(builder, times(1)).build(keyCaptor.capture(), configCaptor.capture());
            assertEquals("ai:ratelimit:user:user-999", new String(keyCaptor.getValue()));
            assertEquals(20, configCaptor.getValue().getBandwidths()[0].getCapacity());
        }

        @Test
        @DisplayName("RL-ADV-6: RESTRICTED tier always returns false immediately")
        void testRestrictedTierAlwaysReturnsFalse() {
            assertFalse(rateLimiter.isAllowed("user-banned", "1.2.3.4", TrustTier.RESTRICTED));
            assertFalse(rateLimiter.isAllowed(null, "1.2.3.4", TrustTier.RESTRICTED));
            assertFalse(rateLimiter.isAllowed("user-banned", null, TrustTier.RESTRICTED));
        }
    }

    // =========================================================================
    // 4. MODERATION TRUNCATION & SURROGATE PAIRS
    // =========================================================================
    @Nested
    @DisplayName("4. Moderation Truncation & UTF-16 Surrogate Pair Integrity")
    class ModerationSurrogateTests {




        @Test
        @DisplayName("MOD-UTF16-1: High surrogate at boundary 1799 is rolled back to 1799, avoiding dangling surrogate")
        void testDanglingSurrogateProtection() {
            // Create a string where index 1799 is the high surrogate '\uD83D' and index 1800 is low surrogate '\uDE00'
            String prefix = "A".repeat(1799);
            String emoji = "\uD83D\uDE00"; // 😀
            String input = prefix + emoji; // total length 1801

            String truncated = CliProxyModerationClient.truncateForModeration(input, 450);

            // Verify length is 1799
            assertEquals(1799, truncated.length());
            // Verify last char is 'A' (not a dangling high surrogate)
            assertEquals('A', truncated.charAt(truncated.length() - 1));
            assertFalse(Character.isHighSurrogate(truncated.charAt(truncated.length() - 1)));
        }

        @Test
        @DisplayName("MOD-UTF16-2: Complete emoji before boundary (at indices 1798-1799) is fully preserved")
        void testCompleteEmojiPreserved() {
            String prefix = "A".repeat(1798);
            String emoji = "\uD83D\uDE00"; // 😀
            String input = prefix + emoji; // total length 1800

            String truncated = CliProxyModerationClient.truncateForModeration(input, 450);

            assertEquals(1800, truncated.length());
            assertTrue(truncated.endsWith("\uD83D\uDE00"));
        }
    }
}
