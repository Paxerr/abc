package com.danasea.backend.modules.order.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.danasea.backend.e2e.BaseE2ETest;
import com.danasea.backend.modules.order.application.dtos.AcceptSubOrderWaiverCommand;
import com.danasea.backend.modules.order.application.dtos.CreatePaymentIntentCommand;
import com.danasea.backend.modules.order.domain.exceptions.WaiverAcceptanceRequiredException;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;
import com.danasea.backend.modules.service.application.dtos.UpdateServiceCommand;
import com.danasea.backend.modules.service.application.usecases.UpdateServiceUseCase;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest(
        properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("test")
class WaiverPersistenceIntegrationTest extends BaseE2ETest {
    @MockitoBean LettuceBasedProxyManager<byte[]> proxyManager;
    @MockitoBean PaymentGatewayPort gateway;
    @Autowired AcceptSubOrderWaiverUseCase accept;
    @Autowired CreatePaymentIntentUseCase payment;
    @Autowired UpdateServiceUseCase update;

    @BeforeEach
    void policy() {
        serviceA1.setWaiverRequired(true);
        serviceA1.setWaiverVersion(1);
        serviceA1.setWaiverContent("Original policy");
        serviceRepository.saveAndFlush(serviceA1);
        unpaidSubOrder.setWaiverRequired(true);
        unpaidSubOrder.setWaiverVersion(1);
        unpaidSubOrder.setWaiverContent("Original policy");
        unpaidSubOrder.setWaiverContentEn(null);
        unpaidSubOrder.setWaiverAccepted(false);
        subOrderRepository.saveAndFlush(unpaidSubOrder);
    }

    UpdateServiceCommand command(String content) {
        return new UpdateServiceCommand(
                vendorUserA.getId(),
                serviceA1.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                content,
                null,
                true,
                null,
                null,
                null);
    }

    @Test
    void concurrentVendorPolicyChangesGetDistinctVersions() throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a =
                    pool.submit(
                            () -> {
                                start.await();
                                return update.execute(command("Policy A")).waiverVersion();
                            });
            var b =
                    pool.submit(
                            () -> {
                                start.await();
                                return update.execute(command("Policy B")).waiverVersion();
                            });
            start.countDown();
            int first = a.get(30, TimeUnit.SECONDS), second = b.get(30, TimeUnit.SECONDS);
            assertNotEquals(first, second);
            assertEquals(5, first + second);
            assertEquals(
                    3,
                    serviceRepository.findById(serviceA1.getId()).orElseThrow().getWaiverVersion());
            assertEquals(
                    "Original policy",
                    subOrderRepository
                            .findById(unpaidSubOrder.getId())
                            .orElseThrow()
                            .getWaiverContent());
        }
    }

    @Test
    void parallelAcceptancesPreserveSingleEvidenceAndActualLanguage() throws Exception {
        var start = new CountDownLatch(1);
        UUID userId = customerUser.getId(), subId = unpaidSubOrder.getId();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a =
                    pool.submit(
                            () -> {
                                start.await();
                                return accept.execute(
                                        userId,
                                        subId,
                                        new AcceptSubOrderWaiverCommand(true, 1, "EN"));
                            });
            var b =
                    pool.submit(
                            () -> {
                                start.await();
                                return accept.execute(
                                        userId,
                                        subId,
                                        new AcceptSubOrderWaiverCommand(true, 1, "EN"));
                            });
            start.countDown();
            var first = a.get(30, TimeUnit.SECONDS);
            var second = b.get(30, TimeUnit.SECONDS);
            assertTrue(
                    Duration.between(first.acceptedAt(), second.acceptedAt()).abs()
                                    .compareTo(Duration.ofNanos(1_000))
                            <= 0);
            assertEquals("VI", first.language());
            var stored = subOrderRepository.findById(subId).orElseThrow();
            assertTrue(stored.getWaiverAccepted());
            assertEquals(userId, stored.getWaiverAcceptedBy());
            assertEquals("Original policy", stored.getWaiverAcceptedContent());
        }
    }

    @Test
    void paymentWithoutAcceptanceNeverContactsGateway() {
        assertThrows(
                WaiverAcceptanceRequiredException.class,
                () ->
                        payment.execute(
                                new CreatePaymentIntentCommand(
                                        customerUser.getId(),
                                        unpaidOrder.getId(),
                                        PaymentProvider.PAYPAL,
                                        "waiver-payment-1")));
        verifyNoInteractions(gateway);
    }

    @Test
    void acceptanceAndPaymentSerializeBeforeGatewayCall() throws Exception {
        UUID userId = customerUser.getId(),
                subId = unpaidSubOrder.getId(),
                orderId = unpaidOrder.getId();
        when(gateway.createPaymentIntent(any(), any(), any(), any()))
                .thenAnswer(
                        inv -> {
                            assertTrue(
                                    subOrderRepository
                                            .findById(subId)
                                            .orElseThrow()
                                            .getWaiverAccepted());
                            return new PaymentIntentResult(
                                    inv.getArgument(0),
                                    orderId,
                                    PaymentProvider.PAYPAL,
                                    inv.getArgument(2),
                                    "https://sandbox.example/approval",
                                    null,
                                    OffsetDateTime.now().plusMinutes(15),
                                    "WAIVER-ORDER-1");
                        });
        var start = new CountDownLatch(1);
        var command =
                new CreatePaymentIntentCommand(
                        userId, orderId, PaymentProvider.PAYPAL, "waiver-payment-race-1");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var accepting =
                    pool.submit(
                            () -> {
                                start.await();
                                return accept.execute(
                                        userId,
                                        subId,
                                        new AcceptSubOrderWaiverCommand(true, 1, "VI"));
                            });
            var paying =
                    pool.submit(
                            () -> {
                                start.await();
                                try {
                                    payment.execute(command);
                                    return true;
                                } catch (WaiverAcceptanceRequiredException missing) {
                                    return false;
                                }
                            });
            start.countDown();
            accepting.get(30, TimeUnit.SECONDS);
            if (!paying.get(30, TimeUnit.SECONDS)) payment.execute(command);
            verify(gateway, times(1))
                    .createPaymentIntent(any(), eq(orderId), any(), eq(PaymentProvider.PAYPAL));
        }
    }
}
