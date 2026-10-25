package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.domain.TrustTier;
import com.danasea.backend.modules.ai.infrastructure.redis.RedisRateLimiter;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.distributed.BucketProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RateLimitAndFallbackTest {

    @Mock
    private LettuceBasedProxyManager<byte[]> proxyManager;
    
    @Mock
    private RemoteBucketBuilder<byte[]> proxyManagerBuilder;
    
    @Mock
    private BucketProxy bucket;

    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(proxyManager.builder()).thenReturn(proxyManagerBuilder);
        when(proxyManagerBuilder.build(any(), any(BucketConfiguration.class))).thenReturn(bucket);
        rateLimiter = new RedisRateLimiter(proxyManager);
    }

    @Test
    void testLayer1SoftThrottle_DegradesToUnverified() {
        // IP bucket fails
        when(bucket.tryConsume(1)).thenReturn(false).thenReturn(true);
        
        boolean allowed = rateLimiter.isAllowed("user1", "127.0.0.1", TrustTier.TRUSTED);
        
        // Since it degrades to UNVERIFIED (capacity 5), the second consume is for UNVERIFIED user token bucket
        assertTrue(allowed);
        // Verify two token buckets were checked (IP then User)
        verify(bucket, times(2)).tryConsume(1);
    }
    
    @Test
    void testRestrictedTierAlwaysBlocked() {
        boolean allowed = rateLimiter.isAllowed("user1", "127.0.0.1", TrustTier.RESTRICTED);
        assertFalse(allowed);
    }
}
