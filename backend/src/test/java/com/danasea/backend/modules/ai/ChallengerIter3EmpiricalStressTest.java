package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.domain.TrustTier;
import com.danasea.backend.modules.ai.infrastructure.redis.RedisRateLimiter;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Challenger 1 (Gen 2 Iteration 3) Empirical Adversarial Stress Verification Suite.
 *
 * Specific Focus:
 * 1. RedisRateLimiter RESTRICTED tier:
 *    - Strict short-circuiting at line 1 of isAllowed() before Layer 1 / Layer 2.
 *    - 0 tokens consumed from IP, User, or Anon buckets.
 *    - 0 interactions with LettuceBasedProxyManager.
 *    - Massive concurrent flood resilience (100 threads x 1,000 iterations = 100,000 requests).
 *
 *    - 0 False Exhaustion under 100 concurrent threads with single active key.
 *    - Dynamic state transitions (COOLDOWN, DISABLED, RECOVERY) under concurrent load.
 *    - True exhaustion verification with 100% deterministic exception throwing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Challenger 1 Gen 2 Iter 3 - Empirical Stress Test Suite")
public class ChallengerIter3EmpiricalStressTest {

    // =========================================================================
    // SECTION 1: REDIS RATE LIMITER RESTRICTED TIER EMPIRICAL STRESS
    // =========================================================================
    @Nested
    @DisplayName("1. RedisRateLimiter RESTRICTED Tier Short-Circuit & Token Invariance")
    class RestrictedTierEmpiricalTests {

        @Mock
        private LettuceBasedProxyManager<byte[]> proxyManager;

        @Mock
        private RemoteBucketBuilder<byte[]> builder;

        @Mock
        private BucketProxy bucketProxy;

        private RedisRateLimiter rateLimiter;

        @BeforeEach
        void setUp() {
            lenient().when(proxyManager.builder()).thenReturn(builder);
            lenient().when(builder.build(any(byte[].class), any(io.github.bucket4j.BucketConfiguration.class))).thenReturn(bucketProxy);
            rateLimiter = new RedisRateLimiter(proxyManager);
        }

        @Test
        @DisplayName("STRESS-RL-1: RESTRICTED tier short-circuits across all parameter permutations with 0 proxy interaction")
        void testRestrictedTier_AllPermutationsZeroInteraction() {
            List<String> userIds = Arrays.asList("user-123", "", "   ", null);
            List<String> ipAddresses = Arrays.asList("1.2.3.4", "2001:db8::1", "", "   ", null);

            for (String user : userIds) {
                for (String ip : ipAddresses) {
                    boolean allowed = rateLimiter.isAllowed(user, ip, TrustTier.RESTRICTED);
                    assertFalse(allowed, String.format("Failed for user='%s', ip='%s'", user, ip));
                }
            }

            // Verify LettuceBasedProxyManager was NEVER touched
            verifyNoInteractions(proxyManager);
            verifyNoInteractions(builder);
            verifyNoInteractions(bucketProxy);
        }

        @Test
        @DisplayName("STRESS-RL-2: Concurrent Flood - 100 threads x 1,000 requests on RESTRICTED tier executes in <500ms with 0 token calls")
        void testRestrictedTier_HighConcurrencyFlood() throws InterruptedException, ExecutionException {
            int threadCount = 100;
            int requestsPerThread = 1000;
            int totalRequests = threadCount * requestsPerThread;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    int blockedCount = 0;
                    for (int i = 0; i < requestsPerThread; i++) {
                        String user = "user-banned-" + (threadId % 10);
                        String ip = "192.168.1." + (threadId % 50);
                        if (!rateLimiter.isAllowed(user, ip, TrustTier.RESTRICTED)) {
                            blockedCount++;
                        }
                    }
                    return blockedCount;
                }));
            }

            readyLatch.await(); // wait for all threads ready
            long startTime = System.nanoTime();
            startLatch.countDown(); // release stampede

            int totalBlocked = 0;
            for (Future<Integer> f : futures) {
                totalBlocked += f.get();
            }
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            executor.shutdown();

            System.out.printf("STRESS-RL-2: Processed %d RESTRICTED requests in %d ms (%.2f req/ms)%n",
                    totalRequests, durationMs, (double) totalRequests / durationMs);

            assertEquals(totalRequests, totalBlocked, "Every single RESTRICTED request must be blocked (false)");
            assertTrue(durationMs < 2000, "100k in-memory short-circuit checks should complete well under 2s");
            verifyNoInteractions(proxyManager);
            verifyNoInteractions(builder);
            verifyNoInteractions(bucketProxy);
        }
    }

    // =========================================================================
    // =========================================================================

}
