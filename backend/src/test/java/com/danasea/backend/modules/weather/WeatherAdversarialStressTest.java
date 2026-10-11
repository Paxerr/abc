package com.danasea.backend.modules.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.danasea.backend.modules.communication.application.dtos.NotificationCommand;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaCategoryRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine;
import com.danasea.backend.modules.weather.infrastructure.jobs.SlotWeatherMonitoringJob;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adversarial Stress & Verification Test Suite for Marine Weather Monitoring System.
 * Created by teamwork_preview_challenger_m4_1 to empirically stress-test:
 * 1. Sliding window precision across exact boundary timestamps (T+23h, T+25h, T+1h, T+3h).
 * 2. Auto-escalation fallback timing: T=61m, T=60m, T=59m.
 * 3. Idempotency under rapid repeat runs: 1 record per slot, in-place metric update, 0 duplicate alerts.
 * 4. YELLOW to RED escalation under worsening weather.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Adversarial Stress Test: Marine Weather Monitoring System")
class WeatherAdversarialStressTest {

    private static NotificationCommand notification(UUID recipientId, String type, String entityType, UUID entityId) {
        return argThat(command -> command != null
                && Objects.equals(recipientId, command.recipientId())
                && type.equals(command.type())
                && entityType.equals(command.relatedEntityType())
                && entityId.equals(command.relatedEntityId()));
    }

    @Mock
    private JpaServiceSlotRepository slotRepository;

    @Mock
    private JpaServiceRepository serviceRepository;

    @Mock
    private JpaCategoryRepository categoryRepository;

    @Mock
    private JpaSafetyRuleEvaluationRepository evaluationRepository;

    @Mock
    private JpaSubOrderRepository subOrderRepository;

    @Mock
    private JpaRefundRepository refundRepository;

    @Mock
    private JpaMasterOrderRepository masterOrderRepository;

    @Mock
    private WeatherProviderPort weatherProviderPort;

    @Spy
    private WeatherRuleEngine weatherRuleEngine = new WeatherRuleEngine();

    @Mock
    private SendNotificationUseCase sendNotificationUseCase;

    private SlotWeatherMonitoringJob job;

    private UUID serviceId;
    private UUID categoryId;
    private UUID slotId;
    private UUID vendorId;
    private UUID customerId;
    private UUID masterOrderId;
    private ServiceSlotJpaEntity slot;
    private ServiceJpaEntity service;
    private CategoryJpaEntity category;

    @BeforeEach
    void setUp() {
        job = new SlotWeatherMonitoringJob(
                slotRepository,
                serviceRepository,
                categoryRepository,
                evaluationRepository,
                subOrderRepository,
                weatherProviderPort,
                weatherRuleEngine,
                sendNotificationUseCase,
                refundRepository,
                null,
                masterOrderRepository
        );

        serviceId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        masterOrderId = UUID.randomUUID();

        slot = new ServiceSlotJpaEntity();
        slot.setId(slotId);
        slot.setServiceId(serviceId);
        slot.setDate(LocalDate.of(2026, 9, 21));
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setBookedCount(3);

        service = new ServiceJpaEntity();
        service.setId(serviceId);
        service.setName("Tour Chèo SUP Mân Thái");
        service.setCategoryId(categoryId);
        service.setVendorId(vendorId);
        service.setLatitude(BigDecimal.valueOf(16.0890));
        service.setLongitude(BigDecimal.valueOf(108.2495));

        category = new CategoryJpaEntity();
        category.setId(categoryId);
        category.setSlug("cheo-sup-kayak");
    }

    // =========================================================================
    // 1. SLIDING WINDOW BOUNDARY PRECISION TESTS
    // =========================================================================
    @Nested
    @DisplayName("1. Sliding Window Precision & Boundary Stress Tests")
    class SlidingWindowBoundaryPrecisionTests {

        @Test
        @DisplayName("Exact Boundary T+23h: slot departure exactly at now + 23h must return TRUE")
        void testExactT23h_returnsTrue() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0, 0);
            LocalDateTime departure = now.plusHours(23);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(departure.toLocalDate());
            testSlot.setStartTime(departure.toLocalTime());

            assertTrue(job.isSlotInSlidingWindow(testSlot, now), "Slot at exact T+23h must be inside sliding window");
        }

        @Test
        @DisplayName("Boundary Violation T+22h 59m 59s: 1 second before T+23h must return FALSE")
        void testJustBeforeT23h_returnsFalse() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0, 0);
            LocalDateTime departure = now.plusHours(23).minusSeconds(1);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(departure.toLocalDate());
            testSlot.setStartTime(departure.toLocalTime());

            assertFalse(job.isSlotInSlidingWindow(testSlot, now), "Slot at T+22h59m59s must be outside sliding window");
        }

        @Test
        @DisplayName("Exact Boundary T+25h: slot departure exactly at now + 25h must return TRUE")
        void testExactT25h_returnsTrue() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0, 0);
            LocalDateTime departure = now.plusHours(25);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(departure.toLocalDate());
            testSlot.setStartTime(departure.toLocalTime());

            assertTrue(job.isSlotInSlidingWindow(testSlot, now), "Slot at exact T+25h must be inside sliding window");
        }

        @Test
        @DisplayName("Boundary Violation T+25h 00m 01s: 1 second after T+25h must return FALSE")
        void testJustAfterT25h_returnsFalse() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0, 0);
            LocalDateTime departure = now.plusHours(25).plusSeconds(1);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(departure.toLocalDate());
            testSlot.setStartTime(departure.toLocalTime());

            assertFalse(job.isSlotInSlidingWindow(testSlot, now), "Slot at T+25h00m01s must be outside sliding window");
        }

        @Test
        @DisplayName("Exact Boundary T+1h: slot departure exactly at now + 1h must return TRUE")
        void testExactT1h_returnsTrue() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0, 0);
            LocalDateTime departure = now.plusHours(1);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(departure.toLocalDate());
            testSlot.setStartTime(departure.toLocalTime());

            assertTrue(job.isSlotInSlidingWindow(testSlot, now), "Slot at exact T+1h must be inside sliding window");
        }

        @Test
        @DisplayName("Boundary Violation T+0h 59m 59s: 1 second before T+1h must return FALSE")
        void testJustBeforeT1h_returnsFalse() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0, 0);
            LocalDateTime departure = now.plusHours(1).minusSeconds(1);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(departure.toLocalDate());
            testSlot.setStartTime(departure.toLocalTime());

            assertFalse(job.isSlotInSlidingWindow(testSlot, now), "Slot at T+0h59m59s must be outside sliding window");
        }

        @Test
        @DisplayName("Exact Boundary T+3h: slot departure exactly at now + 3h must return TRUE")
        void testExactT3h_returnsTrue() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0, 0);
            LocalDateTime departure = now.plusHours(3);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(departure.toLocalDate());
            testSlot.setStartTime(departure.toLocalTime());

            assertTrue(job.isSlotInSlidingWindow(testSlot, now), "Slot at exact T+3h must be inside sliding window");
        }

        @Test
        @DisplayName("Boundary Violation T+3h 00m 01s: 1 second after T+3h must return FALSE")
        void testJustAfterT3h_returnsFalse() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0, 0);
            LocalDateTime departure = now.plusHours(3).plusSeconds(1);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(departure.toLocalDate());
            testSlot.setStartTime(departure.toLocalTime());

            assertFalse(job.isSlotInSlidingWindow(testSlot, now), "Slot at T+3h00m01s must be outside sliding window");
        }

        @Test
        @DisplayName("Gap Between Windows (e.g. T+4h, T+12h, T+22h) must return FALSE")
        void testGapsBetweenWindows_returnsFalse() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 8, 0, 0);

            for (int h : List.of(4, 8, 12, 16, 20, 22, 26, 30)) {
                LocalDateTime departure = now.plusHours(h);
                ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
                testSlot.setDate(departure.toLocalDate());
                testSlot.setStartTime(departure.toLocalTime());
                assertFalse(job.isSlotInSlidingWindow(testSlot, now), "Slot at T+" + h + "h must be outside sliding window");
            }
        }
    }

    // =========================================================================
    // 2. AUTO-ESCALATION FALLBACK TIMING (T=61m, T=60m, T=59m)
    // =========================================================================
    @Nested
    @DisplayName("2. Auto-Escalation Fallback Timing Tests")
    class AutoEscalationFallbackTimingTests {

        private SafetyRuleEvaluationJpaEntity redAlert;
        private SubOrderJpaEntity subOrder;
        private MasterOrderJpaEntity masterOrder;

        @BeforeEach
        void initAlertAndOrder() {
            slot.setDate(LocalDate.of(2026, 9, 20));
            slot.setStartTime(LocalTime.of(9, 0));

            redAlert = new SafetyRuleEvaluationJpaEntity();
            redAlert.setId(UUID.randomUUID());
            redAlert.setSlotId(slotId);
            redAlert.setIsSafe(false);
            redAlert.setAlertLevel("RED");
            redAlert.setStatus("AWAITING_ADMIN_RESOLUTION");
            redAlert.setWarningMessage("Sóng lớn 1.8m nguy hiểm");

            subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setMasterOrderId(masterOrderId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);
            subOrder.setSubtotalAmount(BigDecimal.valueOf(2000000));

            masterOrder = new MasterOrderJpaEntity();
            masterOrder.setId(masterOrderId);
            masterOrder.setCustomerId(customerId);
        }

        @Test
        @DisplayName("T=61m: Remaining time = 61 minutes -> NO auto-cancellation, NO refund")
        void testT61m_noCancellation() {
            // Slot starts at 09:00:00, now is 07:59:00 (exact 61 minutes remaining)
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 7, 59, 0);

            WeatherCancellationFixture.install(job, subOrderRepository, refundRepository, slotRepository, slotId);
        boolean triggered = job.checkAutoEscalationFallback(slot, now);

            assertFalse(triggered, "T=61m must NOT trigger auto-escalation fallback");
            assertEquals(SubOrderStatus.CONFIRMED, subOrder.getStatus());
            verify(subOrderRepository, never()).saveAndFlush(any());
            verify(refundRepository, never()).saveAndFlush(any());
            verify(sendNotificationUseCase, never()).execute(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("T=60m: Remaining time = exactly 60 minutes -> CANCEL sub-order, 100% refund, AUTO_CANCELLED_FOR_SAFETY")
        void testT60m_triggersCancellationAndRefund() {
            // Slot starts at 09:00:00, now is 08:00:00 (exact 60 minutes remaining)
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 8, 0, 0);

            when(evaluationRepository.findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(eq(slotId), anyList()))
                    .thenReturn(Optional.of(redAlert));
            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));

            WeatherCancellationFixture.install(job, subOrderRepository, refundRepository, slotRepository, slotId);
        boolean triggered = job.checkAutoEscalationFallback(slot, now);

            assertTrue(triggered, "T=60m MUST trigger auto-escalation fallback");
            assertEquals(SubOrderStatus.CANCELLED, subOrder.getStatus());
            verify(subOrderRepository, times(1)).saveAndFlush(subOrder);

            // Verify 100% refund
            ArgumentCaptor<RefundJpaEntity> refundCaptor = ArgumentCaptor.forClass(RefundJpaEntity.class);
            verify(refundRepository, times(1)).saveAndFlush(refundCaptor.capture());
            RefundJpaEntity refund = refundCaptor.getValue();
            assertEquals(BigDecimal.valueOf(2000000), refund.getAmount());
            assertEquals(BigDecimal.valueOf(100.0), refund.getRefundPercentage());
            assertEquals(RefundReason.WEATHER, refund.getReason());
            assertEquals(RefundStatus.PENDING, refund.getStatus());

            // Verify alert status updated
            assertEquals("AUTO_CANCELLED_FOR_SAFETY", redAlert.getStatus());
            assertFalse(redAlert.getIsSafe());
            verify(evaluationRepository, times(1)).save(redAlert);

            // Verify tri-party notifications (Admin, Vendor, Customer)
            verify(sendNotificationUseCase).execute(notification(null, "AUTO_CANCELLED_FOR_SAFETY", "SERVICE_SLOT", slotId));
            verify(sendNotificationUseCase).execute(notification(vendorId, "AUTO_CANCELLED_FOR_SAFETY", "SERVICE_SLOT", slotId));
            verify(sendNotificationUseCase).execute(notification(customerId, "AUTO_CANCELLED_FOR_SAFETY", "SUB_ORDER", subOrder.getId()));
        }

        @Test
        @DisplayName("T=59m: Remaining time = 59 minutes -> CANCEL sub-order, 100% refund, AUTO_CANCELLED_FOR_SAFETY")
        void testT59m_triggersCancellationAndRefund() {
            // Slot starts at 09:00:00, now is 08:01:00 (59 minutes remaining)
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 8, 1, 0);

            when(evaluationRepository.findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(eq(slotId), anyList()))
                    .thenReturn(Optional.of(redAlert));
            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));

            WeatherCancellationFixture.install(job, subOrderRepository, refundRepository, slotRepository, slotId);
        boolean triggered = job.checkAutoEscalationFallback(slot, now);

            assertTrue(triggered, "T=59m MUST trigger auto-escalation fallback");
            assertEquals(SubOrderStatus.CANCELLED, subOrder.getStatus());
            verify(subOrderRepository, times(1)).saveAndFlush(subOrder);
            verify(refundRepository, times(1)).saveAndFlush(any(RefundJpaEntity.class));
            assertEquals("AUTO_CANCELLED_FOR_SAFETY", redAlert.getStatus());
        }

        @Test
        @DisplayName("T < 0m: Slot already departed in the past -> Does NOT trigger auto-escalation")
        void testPastDeparture_doesNotTrigger() {
            // Slot started at 09:00, now is 09:05 (5 minutes after start)
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 9, 5, 0);

            WeatherCancellationFixture.install(job, subOrderRepository, refundRepository, slotRepository, slotId);
        boolean triggered = job.checkAutoEscalationFallback(slot, now);

            assertFalse(triggered, "Past slot must NOT trigger auto-escalation");
            verify(subOrderRepository, never()).saveAndFlush(any());
            verify(refundRepository, never()).saveAndFlush(any());
        }
    }

    // =========================================================================
    // 3. IDEMPOTENCY UNDER RAPID REPEAT RUNS
    // =========================================================================
    @Nested
    @DisplayName("3. Idempotency & In-Place Metric Update Stress Tests")
    class IdempotencyStressTests {

        @Test
        @DisplayName("Rapid Repeat Runs (RED): Exactly 1 alert record per slot, in-place metric update, 0 duplicate alerts")
        void testRapidRepeatRuns_redWeather_strictIdempotency() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            WeatherInfoDto.TimeWindowForecast redForecastRun1 = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(1.2)
                    .peakWindSpeed(22.0)
                    .peakWindGust(28.0)
                    .peakOceanCurrent(0.2)
                    .minVisibility(3500.0)
                    .severeWeatherCode(0)
                    .build();

            WeatherInfoDto.TimeWindowForecast redForecastRun2 = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(1.35) // Metric slightly increased
                    .peakWindSpeed(23.0)
                    .peakWindGust(29.0)
                    .peakOceanCurrent(0.25)
                    .minVisibility(3200.0)
                    .severeWeatherCode(0)
                    .build();

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);
            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            // Setup simulated in-memory store for evaluationRepository
            Map<UUID, SafetyRuleEvaluationJpaEntity> dbStore = new ConcurrentHashMap<>();

            doAnswer(invocation -> {
                SafetyRuleEvaluationJpaEntity entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                dbStore.put(entity.getSlotId(), entity);
                return entity;
            }).when(evaluationRepository).save(any(SafetyRuleEvaluationJpaEntity.class));

            when(evaluationRepository.findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(eq(slotId), anyList()))
                    .thenAnswer(invocation -> Optional.ofNullable(dbStore.get(slotId)));

            // RUN 1: First scan creates alert
            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(redForecastRun1);

            boolean run1 = job.evaluateAndAlertSlot(slot);
            assertTrue(run1);
            assertEquals(1, dbStore.size(), "Should have exactly 1 record in store after Run 1");
            SafetyRuleEvaluationJpaEntity stored = dbStore.get(slotId);
            assertEquals("RED", stored.getAlertLevel());
            assertEquals("AWAITING_ADMIN_RESOLUTION", stored.getStatus());
            assertEquals(1.2, stored.getPeakWaveHeightM());

            // Initial notifications: 1 to vendor, 1 to customer
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_ALERT", "SERVICE_SLOT", slotId));
            verify(sendNotificationUseCase).execute(notification(null, "WEATHER_WARNING", "SUB_ORDER", subOrder.getId()));

            // RUN 2: Rapid repeat scan 1 minute later with worsening metrics
            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(redForecastRun2);

            boolean run2 = job.evaluateAndAlertSlot(slot);
            assertTrue(run2);
            assertEquals(1, dbStore.size(), "Must NOT create duplicate record in Run 2!");
            // Verify in-place update
            assertEquals(1.35, stored.getPeakWaveHeightM(), "Metric peakWaveHeightM must be updated in-place!");
            assertEquals(23.0, stored.getPeakWindSpeedKmh());

            // RUN 3: Rapid repeat scan 2 minutes later with same metrics
            boolean run3 = job.evaluateAndAlertSlot(slot);
            assertTrue(run3);
            assertEquals(1, dbStore.size(), "Must NOT create duplicate record in Run 3!");

            // VERIFY: Notifications were NOT duplicated! Still exactly 1 call each!
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_ALERT", "SERVICE_SLOT", slotId));
            verify(sendNotificationUseCase).execute(notification(null, "WEATHER_WARNING", "SUB_ORDER", subOrder.getId()));
        }

        @Test
        @DisplayName("Rapid Repeat Runs (YELLOW): Exactly 1 alert record per slot, in-place metric update, 0 duplicate alerts")
        void testRapidRepeatRuns_yellowWeather_strictIdempotency() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            WeatherInfoDto.TimeWindowForecast yellowForecast1 = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(0.6)
                    .peakWindSpeed(12.0)
                    .peakWindGust(16.0)
                    .peakOceanCurrent(0.15)
                    .minVisibility(4500.0)
                    .severeWeatherCode(0)
                    .build();

            WeatherInfoDto.TimeWindowForecast yellowForecast2 = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(0.7) // Changed caution wave
                    .peakWindSpeed(14.0)
                    .peakWindGust(18.0)
                    .peakOceanCurrent(0.18)
                    .minVisibility(4000.0)
                    .severeWeatherCode(0)
                    .build();

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);
            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            Map<UUID, SafetyRuleEvaluationJpaEntity> dbStore = new ConcurrentHashMap<>();

            doAnswer(invocation -> {
                SafetyRuleEvaluationJpaEntity entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                dbStore.put(entity.getSlotId(), entity);
                return entity;
            }).when(evaluationRepository).save(any(SafetyRuleEvaluationJpaEntity.class));

            when(evaluationRepository.findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(eq(slotId), anyList()))
                    .thenAnswer(invocation -> Optional.ofNullable(dbStore.get(slotId)));

            // RUN 1: First scan creates YELLOW alert
            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(yellowForecast1);

            boolean run1 = job.evaluateAndAlertSlot(slot);
            assertTrue(run1);
            assertEquals(1, dbStore.size());
            SafetyRuleEvaluationJpaEntity stored = dbStore.get(slotId);
            assertEquals("YELLOW", stored.getAlertLevel());
            assertEquals("MONITORING_YELLOW", stored.getStatus());
            assertEquals(0.6, stored.getPeakWaveHeightM());

            // 1 notification each to vendor & customer
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_EARLY_WARNING", "SERVICE_SLOT", slotId));

            // RUN 2: Second scan with updated metrics
            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(yellowForecast2);

            boolean run2 = job.evaluateAndAlertSlot(slot);
            assertTrue(run2);
            assertEquals(1, dbStore.size(), "Must maintain exactly 1 record!");
            assertEquals(0.7, stored.getPeakWaveHeightM(), "Must update metrics in-place!");

            // ZERO duplicate early warnings
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_EARLY_WARNING", "SERVICE_SLOT", slotId));
        }
    }

    // =========================================================================
    // 4. YELLOW TO RED ESCALATION UNDER WORSENING WEATHER
    // =========================================================================
    @Nested
    @DisplayName("4. YELLOW to RED Escalation Tests")
    class YellowToRedEscalationTests {

        @Test
        @DisplayName("Weather worsens from YELLOW to RED: In-place update to AWAITING_ADMIN_RESOLUTION, RED, and dispatch urgent alerts")
        void testYellowToRedEscalation_flowAndNotifications() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setMasterOrderId(masterOrderId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);
            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
            masterOrder.setId(masterOrderId);
            masterOrder.setCustomerId(customerId);
            when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));

            Map<UUID, SafetyRuleEvaluationJpaEntity> dbStore = new ConcurrentHashMap<>();

            doAnswer(invocation -> {
                SafetyRuleEvaluationJpaEntity entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                dbStore.put(entity.getSlotId(), entity);
                return entity;
            }).when(evaluationRepository).save(any(SafetyRuleEvaluationJpaEntity.class));

            when(evaluationRepository.findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(eq(slotId), anyList()))
                    .thenAnswer(invocation -> Optional.ofNullable(dbStore.get(slotId)));

            // STEP 1: Scan at T-24h -> YELLOW Caution
            WeatherInfoDto.TimeWindowForecast cautionForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(0.65) // Between caution 0.5m and max 0.8m
                    .peakWindSpeed(12.0)
                    .peakWindGust(16.0)
                    .peakOceanCurrent(0.2)
                    .minVisibility(5000.0)
                    .severeWeatherCode(0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(cautionForecast);

            boolean t24Alert = job.evaluateAndAlertSlot(slot);
            assertTrue(t24Alert);
            assertEquals(1, dbStore.size());
            SafetyRuleEvaluationJpaEntity alert = dbStore.get(slotId);
            assertEquals("YELLOW", alert.getAlertLevel());
            assertEquals("MONITORING_YELLOW", alert.getStatus());
            assertTrue(alert.getIsSafe());

            // Early warnings sent
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_EARLY_WARNING", "SERVICE_SLOT", slotId));
            verify(sendNotificationUseCase).execute(notification(customerId, "WEATHER_EARLY_WARNING", "SUB_ORDER", subOrder.getId()));
            // Admin NOT notified on yellow
            verify(sendNotificationUseCase, never()).execute(notification(null, "WEATHER_ALERT", "SERVICE_SLOT", slotId));

            // STEP 2: Scan at T-2h -> Sudden Squall / Severe Weather (RED)
            WeatherInfoDto.TimeWindowForecast stormForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(1.5) // Exceeds max 0.8m
                    .peakWindSpeed(28.0)
                    .peakWindGust(40.0)
                    .peakOceanCurrent(0.4)
                    .minVisibility(1500.0)
                    .severeWeatherCode(95) // Thunderstorm
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(stormForecast);

            boolean t2Alert = job.evaluateAndAlertSlot(slot);
            assertTrue(t2Alert);

            // Exactly 1 record exists, updated in-place!
            assertEquals(1, dbStore.size(), "Escalation must NOT create a separate record, must update in-place!");
            assertEquals("RED", alert.getAlertLevel(), "Alert level must escalate to RED");
            assertEquals("AWAITING_ADMIN_RESOLUTION", alert.getStatus(), "Status must escalate to AWAITING_ADMIN_RESOLUTION");
            assertFalse(alert.getIsSafe(), "isSafe must become FALSE");
            assertEquals(1.5, alert.getPeakWaveHeightM());
            assertEquals(95, alert.getSevereWeatherCode());

            // Escalation notifications sent:
            // 1. Vendor with [LEO THANG NGUY HIỂM]
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_ALERT", "SERVICE_SLOT", slotId));
            // 2. Customer with [LEO THANG NGUY HIỂM]
            verify(sendNotificationUseCase).execute(notification(customerId, "WEATHER_WARNING", "SUB_ORDER", subOrder.getId()));
            // 3. Admin urgent alert
            verify(sendNotificationUseCase).execute(notification(null, "WEATHER_ALERT", "SERVICE_SLOT", slotId));

            // STEP 3: Repeat run after escalation -> Stays at RED, in-place update, NO extra alerts
            boolean t2RepeatAlert = job.evaluateAndAlertSlot(slot);
            assertTrue(t2RepeatAlert);
            assertEquals(1, dbStore.size());
            // Notifications counts remain unchanged
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_ALERT", "SERVICE_SLOT", slotId));
        }
    }
}
