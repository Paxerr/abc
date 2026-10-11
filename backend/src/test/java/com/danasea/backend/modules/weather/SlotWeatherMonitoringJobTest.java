package com.danasea.backend.modules.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlotWeatherMonitoringJob Comprehensive Tests")
class SlotWeatherMonitoringJobTest {

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

    @InjectMocks
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
        serviceId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        masterOrderId = UUID.randomUUID();

        slot = new ServiceSlotJpaEntity();
        slot.setId(slotId);
        slot.setServiceId(serviceId);
        slot.setDate(LocalDate.now().plusDays(1));
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setBookedCount(3);

        service = new ServiceJpaEntity();
        service.setId(serviceId);
        service.setName("Tour Chèo SUP Ngắm Bình Minh");
        service.setCategoryId(categoryId);
        service.setVendorId(vendorId);
        service.setLatitude(BigDecimal.valueOf(16.0890));
        service.setLongitude(BigDecimal.valueOf(108.2495));

        category = new CategoryJpaEntity();
        category.setId(categoryId);
        category.setSlug("cheo-sup-kayak");
    }

    @Nested
    @DisplayName("1. Sliding Window Scheduler & Precision Tests")
    class SlidingWindowTests {

        @Test
        @DisplayName("Slot khởi hành ở T-24h (24 giờ tới) -> Nằm trong cửa sổ trượt [now+23h, now+25h]")
        void testSlidingWindow_slotInT24Window_returnsTrue() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0);
            LocalDateTime slotDeparture = now.plusHours(24);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(slotDeparture.toLocalDate());
            testSlot.setStartTime(slotDeparture.toLocalTime());

            assertTrue(job.isSlotInSlidingWindow(testSlot, now));
        }

        @Test
        @DisplayName("Slot khởi hành ở T-2h (2 giờ tới) -> Nằm trong cửa sổ trượt [now+1h, now+3h]")
        void testSlidingWindow_slotInT2Window_returnsTrue() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0);
            LocalDateTime slotDeparture = now.plusHours(2);

            ServiceSlotJpaEntity testSlot = new ServiceSlotJpaEntity();
            testSlot.setDate(slotDeparture.toLocalDate());
            testSlot.setStartTime(slotDeparture.toLocalTime());

            assertTrue(job.isSlotInSlidingWindow(testSlot, now));
        }

        @Test
        @DisplayName("Slot khởi hành ở mốc thời gian ngoài cửa sổ (10h tới hoặc 36h tới) -> Trả về false")
        void testSlidingWindow_slotOutsideWindows_returnsFalse() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 10, 0);

            ServiceSlotJpaEntity slot10h = new ServiceSlotJpaEntity();
            slot10h.setDate(now.plusHours(10).toLocalDate());
            slot10h.setStartTime(now.plusHours(10).toLocalTime());

            ServiceSlotJpaEntity slot36h = new ServiceSlotJpaEntity();
            slot36h.setDate(now.plusHours(36).toLocalDate());
            slot36h.setStartTime(now.plusHours(36).toLocalTime());

            assertFalse(job.isSlotInSlidingWindow(slot10h, now));
            assertFalse(job.isSlotInSlidingWindow(slot36h, now));
        }

        @Test
        @DisplayName("Kiểm tra giá trị biên chính xác của cửa sổ trượt (exact boundary 23h, 25h, 1h, 3h)")
        void testSlidingWindow_boundaryConditions_returnsTrue() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 14, 0);

            // Boundary T-24h: now+23h và now+25h
            ServiceSlotJpaEntity exact23h = new ServiceSlotJpaEntity();
            exact23h.setDate(now.plusHours(23).toLocalDate());
            exact23h.setStartTime(now.plusHours(23).toLocalTime());
            assertTrue(job.isSlotInSlidingWindow(exact23h, now));

            ServiceSlotJpaEntity exact25h = new ServiceSlotJpaEntity();
            exact25h.setDate(now.plusHours(25).toLocalDate());
            exact25h.setStartTime(now.plusHours(25).toLocalTime());
            assertTrue(job.isSlotInSlidingWindow(exact25h, now));

            // Boundary T-2h: now+1h và now+3h
            ServiceSlotJpaEntity exact1h = new ServiceSlotJpaEntity();
            exact1h.setDate(now.plusHours(1).toLocalDate());
            exact1h.setStartTime(now.plusHours(1).toLocalTime());
            assertTrue(job.isSlotInSlidingWindow(exact1h, now));

            ServiceSlotJpaEntity exact3h = new ServiceSlotJpaEntity();
            exact3h.setDate(now.plusHours(3).toLocalDate());
            exact3h.setStartTime(now.plusHours(3).toLocalTime());
            assertTrue(job.isSlotInSlidingWindow(exact3h, now));
        }

        @Test
        @DisplayName("Bao phủ các khung giờ khởi hành linh hoạt trong ngày (06:00, 11:30, 15:00, 19:00)")
        void testSlidingWindow_flexibleTimesOfDay() {
            // Test slot sáng sớm 06:00
            LocalDateTime nowEarly = LocalDateTime.of(2026, 9, 20, 6, 0);
            ServiceSlotJpaEntity slot0600 = new ServiceSlotJpaEntity();
            slot0600.setDate(LocalDate.of(2026, 9, 21));
            slot0600.setStartTime(LocalTime.of(6, 0));
            assertTrue(job.isSlotInSlidingWindow(slot0600, nowEarly));

            // Test slot trưa 11:30
            LocalDateTime nowNoon = LocalDateTime.of(2026, 9, 20, 11, 30);
            ServiceSlotJpaEntity slot1130 = new ServiceSlotJpaEntity();
            slot1130.setDate(LocalDate.of(2026, 9, 21));
            slot1130.setStartTime(LocalTime.of(11, 30));
            assertTrue(job.isSlotInSlidingWindow(slot1130, nowNoon));

            // Test slot chiều 15:00
            LocalDateTime nowAfternoon = LocalDateTime.of(2026, 9, 20, 13, 0);
            ServiceSlotJpaEntity slot1500 = new ServiceSlotJpaEntity();
            slot1500.setDate(LocalDate.of(2026, 9, 20));
            slot1500.setStartTime(LocalTime.of(15, 0)); // 2h sau now
            assertTrue(job.isSlotInSlidingWindow(slot1500, nowAfternoon));
        }
    }

    @Nested
    @DisplayName("2. Weather Evaluation, Early Warning & Idempotency Tests")
    class WeatherEvaluationTests {

        @Test
        @DisplayName("evaluateAndAlertSlot: Thời tiết tốt -> Không kích hoạt cảnh báo")
        void evaluateAndAlertSlot_safeWeather_returnsFalse() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            WeatherInfoDto.TimeWindowForecast safeForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(0.3)
                    .peakWindSpeed(10.0)
                    .peakWindGust(15.0)
                    .peakOceanCurrent(0.1)
                    .minVisibility(5000.0)
                    .severeWeatherCode(0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(safeForecast);

            boolean alerted = job.evaluateAndAlertSlot(slot);

            assertFalse(alerted);
            verify(evaluationRepository, never()).save(any());
            verify(sendNotificationUseCase, never()).execute(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("evaluateAndAlertSlot: Sóng 1.2m vượt ngưỡng SUP (0.8m) -> Kích hoạt cảnh báo RED, lưu DB và gửi thông báo")
        void evaluateAndAlertSlot_hazardousWeather_triggersAlertAndNotifications() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            WeatherInfoDto.TimeWindowForecast hazardousForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(1.2)
                    .peakWindSpeed(15.0)
                    .peakWindGust(20.0)
                    .peakOceanCurrent(0.2)
                    .minVisibility(4000.0)
                    .severeWeatherCode(0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(hazardousForecast);

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);

            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            boolean alerted = job.evaluateAndAlertSlot(slot);

            assertTrue(alerted);
            verify(evaluationRepository, times(1)).save(any());
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_ALERT", "SERVICE_SLOT", slotId));
            verify(sendNotificationUseCase).execute(notification(null, "WEATHER_WARNING", "SUB_ORDER", subOrder.getId()));
        }

        @Test
        @DisplayName("evaluateAndAlertSlot: Sóng 0.6m chạm ngưỡng YELLOW -> Lưu MONITORING_YELLOW và gửi Early Warning")
        void evaluateAndAlertSlot_cautionWeather_triggersEarlyWarning() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            WeatherInfoDto.TimeWindowForecast cautionForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(0.6)
                    .peakWindSpeed(10.0)
                    .peakWindGust(15.0)
                    .peakOceanCurrent(0.1)
                    .minVisibility(4000.0)
                    .severeWeatherCode(0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(cautionForecast);

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);

            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            boolean alerted = job.evaluateAndAlertSlot(slot);

            assertTrue(alerted);
            ArgumentCaptor<SafetyRuleEvaluationJpaEntity> captor =
                    ArgumentCaptor.forClass(SafetyRuleEvaluationJpaEntity.class);
            verify(evaluationRepository, times(1)).save(captor.capture());
            var savedEntity = captor.getValue();
            assertEquals("YELLOW", savedEntity.getAlertLevel());
            assertEquals("MONITORING_YELLOW", savedEntity.getStatus());
            assertTrue(savedEntity.getIsSafe());

            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_EARLY_WARNING", "SERVICE_SLOT", slotId));
        }

        @Test
        @DisplayName("Idempotency: Cảnh báo RED active giữ nguyên mức độ -> Cập nhật in-place, không spam thông báo")
        void evaluateAndAlertSlot_idempotency_doesNotSpam() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            WeatherInfoDto.TimeWindowForecast hazardousForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(1.2)
                    .peakWindSpeed(15.0)
                    .peakWindGust(20.0)
                    .peakOceanCurrent(0.2)
                    .minVisibility(4000.0)
                    .severeWeatherCode(0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(hazardousForecast);

            var existingEntity = new SafetyRuleEvaluationJpaEntity();
            existingEntity.setId(UUID.randomUUID());
            existingEntity.setSlotId(slotId);
            existingEntity.setIsSafe(false);
            existingEntity.setAlertLevel("RED");
            existingEntity.setStatus("AWAITING_ADMIN_RESOLUTION");

            when(evaluationRepository.findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(eq(slotId), anyList()))
                    .thenReturn(Optional.of(existingEntity));

            boolean alerted = job.evaluateAndAlertSlot(slot);

            assertTrue(alerted);
            verify(evaluationRepository, times(1)).save(existingEntity);
            verify(sendNotificationUseCase, never()).execute(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("Escalation: Thời tiết xấu đi từ YELLOW lên RED -> Cập nhật status thành AWAITING_ADMIN_RESOLUTION và gửi cảnh báo khẩn cấp 3 bên")
        void evaluateAndAlertSlot_escalationYellowToRed_triggersUrgentEscalationAlerts() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            // Đợt bão giật cấp 6 (RED)
            WeatherInfoDto.TimeWindowForecast stormForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(1.4) // Cấm đỏ SUP
                    .peakWindSpeed(25.0)
                    .peakWindGust(35.0)
                    .peakOceanCurrent(0.3)
                    .minVisibility(2000.0)
                    .severeWeatherCode(95) // Dông sét
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(stormForecast);

            // Giả lập slot trước đó đang theo dõi mức YELLOW
            var existingYellowAlert = new SafetyRuleEvaluationJpaEntity();
            existingYellowAlert.setId(UUID.randomUUID());
            existingYellowAlert.setSlotId(slotId);
            existingYellowAlert.setIsSafe(true);
            existingYellowAlert.setAlertLevel("YELLOW");
            existingYellowAlert.setStatus("MONITORING_YELLOW");

            when(evaluationRepository.findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(eq(slotId), anyList()))
                    .thenReturn(Optional.of(existingYellowAlert));

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);
            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            boolean alerted = job.evaluateAndAlertSlot(slot);

            assertTrue(alerted);
            // Verify cập nhật in-place sang RED & AWAITING_ADMIN_RESOLUTION
            assertEquals("RED", existingYellowAlert.getAlertLevel());
            assertEquals("AWAITING_ADMIN_RESOLUTION", existingYellowAlert.getStatus());
            assertFalse(existingYellowAlert.getIsSafe());
            verify(evaluationRepository, times(1)).save(existingYellowAlert);

            // Verify bắn thông báo leo thang cho Vendor (chứa tiền tố leo thang)
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_ALERT", "SERVICE_SLOT", slotId));

            // Verify bắn thông báo leo thang cho Customer
            verify(sendNotificationUseCase).execute(notification(null, "WEATHER_WARNING", "SUB_ORDER", subOrder.getId()));

            // Verify bắn thông báo leo thang cho Admin
            verify(sendNotificationUseCase).execute(notification(null, "WEATHER_ALERT", "SERVICE_SLOT", slotId));
        }

        @Test
        @DisplayName("Customer ID Resolution: Tra cứu thành công MasterOrderJpaEntity -> userId gửi thông báo không bao giờ bị null")
        void evaluateAndAlertSlot_customerIdResolution_resolvesFromMasterOrder() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            WeatherInfoDto.TimeWindowForecast redForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(1.5)
                    .peakWindSpeed(20.0)
                    .peakWindGust(25.0)
                    .peakOceanCurrent(0.2)
                    .minVisibility(3000.0)
                    .severeWeatherCode(0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(redForecast);

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

            boolean alerted = job.evaluateAndAlertSlot(slot);

            assertTrue(alerted);
            // Verify notification gửi tới customerId thật thay vì null
            verify(sendNotificationUseCase).execute(notification(customerId, "WEATHER_WARNING", "SUB_ORDER", subOrder.getId()));
        }
    }

    @Nested
    @DisplayName("3. Auto-Escalation Fallback (T <= 60 Minutes) Tests")
    class AutoEscalationFallbackTests {

        @Test
        @DisplayName("Khi còn 45 phút trước khởi hành mà cảnh báo RED chưa được giải quyết -> Tự động hủy slot, hoàn tiền 100% và đánh dấu AUTO_CANCELLED_FOR_SAFETY")
        void checkAutoEscalationFallback_under60Minutes_triggersCancellationAndRefund() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 20, 7, 15);
            // Slot khởi hành lúc 8:00 (còn 45 phút)
            slot.setDate(LocalDate.of(2026, 9, 20));
            slot.setStartTime(LocalTime.of(8, 0));

            SafetyRuleEvaluationJpaEntity redAlert = new SafetyRuleEvaluationJpaEntity();
            redAlert.setId(UUID.randomUUID());
            redAlert.setSlotId(slotId);
            redAlert.setIsSafe(false);
            redAlert.setAlertLevel("RED");
            redAlert.setStatus("AWAITING_ADMIN_RESOLUTION");
            redAlert.setWarningMessage("Sóng biển 1.8m giật cấp 7");

            when(evaluationRepository.findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(eq(slotId), anyList()))
                    .thenReturn(Optional.of(redAlert));

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setMasterOrderId(masterOrderId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);
            subOrder.setSubtotalAmount(BigDecimal.valueOf(1500000));
            subOrder.setFinalAmount(BigDecimal.valueOf(1200000));

            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
            masterOrder.setId(masterOrderId);
            masterOrder.setCustomerId(customerId);
            when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));

            WeatherCancellationFixture.install(job, subOrderRepository, refundRepository, slotRepository, slotId);
        boolean fallbackTriggered = job.checkAutoEscalationFallback(slot, now);

            assertTrue(fallbackTriggered);

            // 1. SubOrder chuyển sang CANCELLED
            assertEquals(SubOrderStatus.CANCELLED, subOrder.getStatus());
            verify(subOrderRepository, times(1)).saveAndFlush(subOrder);

            // 2. Tạo bản ghi hoàn tiền 100% với RefundReason.WEATHER và PROCESSED
            ArgumentCaptor<RefundJpaEntity> refundCaptor = ArgumentCaptor.forClass(RefundJpaEntity.class);
            verify(refundRepository, times(1)).saveAndFlush(refundCaptor.capture());
            RefundJpaEntity refund = refundCaptor.getValue();
            assertEquals(BigDecimal.valueOf(1200000), refund.getAmount());
            assertEquals(BigDecimal.valueOf(100.0), refund.getRefundPercentage());
            assertEquals(RefundReason.WEATHER, refund.getReason());
            assertEquals(RefundStatus.PENDING, refund.getStatus());

            // 3. Trạng thái bản ghi sự cố cập nhật sang AUTO_CANCELLED_FOR_SAFETY
            assertEquals("AUTO_CANCELLED_FOR_SAFETY", redAlert.getStatus());
            assertFalse(redAlert.getIsSafe());
            verify(evaluationRepository, times(1)).save(redAlert);

            // 4. Bắn thông báo khẩn cấp đồng thời 3 bên
            // Admin
            verify(sendNotificationUseCase).execute(notification(null, "AUTO_CANCELLED_FOR_SAFETY", "SERVICE_SLOT", slotId));

            // Vendor
            verify(sendNotificationUseCase).execute(notification(vendorId, "AUTO_CANCELLED_FOR_SAFETY", "SERVICE_SLOT", slotId));

            // Customer
            verify(sendNotificationUseCase).execute(notification(customerId, "AUTO_CANCELLED_FOR_SAFETY", "SUB_ORDER", subOrder.getId()));
        }

        @Test
        @DisplayName("Khi còn trên 12 giờ trước giờ khởi hành -> Không kích hoạt Auto-escalation fallback")
        void checkAutoEscalationFallback_over12Hours_doesNotCancel() {
            LocalDateTime now = LocalDateTime.of(2026, 9, 19, 18, 0);
            // Slot khởi hành lúc 8:00 (còn 120 phút)
            slot.setDate(LocalDate.of(2026, 9, 20));
            slot.setStartTime(LocalTime.of(8, 0));

            WeatherCancellationFixture.install(job, subOrderRepository, refundRepository, slotRepository, slotId);
        boolean fallbackTriggered = job.checkAutoEscalationFallback(slot, now);

            assertFalse(fallbackTriggered);
            verify(subOrderRepository, never()).saveAndFlush(any());
            verify(refundRepository, never()).saveAndFlush(any());
            verify(sendNotificationUseCase, never()).execute(any(NotificationCommand.class));
        }
    }
}
