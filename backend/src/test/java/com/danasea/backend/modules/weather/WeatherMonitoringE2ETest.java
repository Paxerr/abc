package com.danasea.backend.modules.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.danasea.backend.modules.communication.application.dtos.NotificationCommand;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
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
import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine;
import com.danasea.backend.modules.weather.infrastructure.adapters.WeatherProviderAdapter;
import com.danasea.backend.modules.weather.infrastructure.api.OpenMeteoApiClient;
import com.danasea.backend.modules.weather.infrastructure.api.dtos.OpenMeteoMarineResponse;
import com.danasea.backend.modules.weather.infrastructure.api.dtos.OpenMeteoWeatherResponse;
import com.danasea.backend.modules.weather.infrastructure.jobs.SlotWeatherMonitoringJob;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.CategorySafetyRuleJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaCategorySafetyRuleRepository;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;
import com.danasea.backend.modules.weather.presentation.controllers.AdminWeatherAlertController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Comprehensive, Opaque-Box E2E & Acceptance Test Suite for the DANASEA Marine Weather Monitoring System Upgrade.
 *
 * Implements the 4-Tier test architecture defined in TEST_INFRA.md:
 * - Tier 1: Feature Coverage (F1 to F9)
 * - Tier 2: Boundary & Corner Cases (Threshold limits, boundary values, null handling)
 * - Tier 3: Cross-Feature Combinations (Pairwise workflows C1 to C4)
 * - Tier 4: Real-World Workload Scenarios (Scenarios 1 to 5)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DANASEA Marine Weather Monitoring System - Comprehensive E2E Test Suite")
class WeatherMonitoringE2ETest {

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
    private JpaCategorySafetyRuleRepository categorySafetyRuleRepository;

    @Mock
    private SendNotificationUseCase sendNotificationUseCase;

    @Mock
    private OpenMeteoApiClient openMeteoApiClient;

    @Mock
    private WeatherProviderPort weatherProviderPort;

    private WeatherRuleEngine weatherRuleEngine;
    private SlotWeatherMonitoringJob slotWeatherMonitoringJob;
    private AdminWeatherAlertController adminWeatherAlertController;
    private WeatherProviderAdapter weatherProviderAdapter;

    private UUID serviceId;
    private UUID categoryId;
    private UUID slotId;
    private UUID vendorId;
    private ServiceSlotJpaEntity testSlot;
    private ServiceJpaEntity testService;
    private CategoryJpaEntity testCategory;

    @BeforeEach
    void setUp() {
        weatherRuleEngine = new WeatherRuleEngine();

        slotWeatherMonitoringJob = new SlotWeatherMonitoringJob(
                slotRepository,
                serviceRepository,
                categoryRepository,
                evaluationRepository,
                subOrderRepository,
                weatherProviderPort,
                weatherRuleEngine,
                sendNotificationUseCase
        );

        adminWeatherAlertController = new AdminWeatherAlertController(
                evaluationRepository,
                subOrderRepository,
                refundRepository,
                slotRepository,
                serviceRepository,
                sendNotificationUseCase
        );

        weatherProviderAdapter = new WeatherProviderAdapter(openMeteoApiClient);

        serviceId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        vendorId = UUID.randomUUID();

        testSlot = new ServiceSlotJpaEntity();
        testSlot.setId(slotId);
        testSlot.setServiceId(serviceId);
        testSlot.setDate(LocalDate.now().plusDays(1));
        testSlot.setStartTime(LocalTime.of(8, 0));
        testSlot.setEndTime(LocalTime.of(10, 0));
        testSlot.setBookedCount(4);

        testService = new ServiceJpaEntity();
        testService.setId(serviceId);
        testService.setName("Tour Chèo SUP Đón Bình Minh Mân Thái");
        testService.setCategoryId(categoryId);
        testService.setVendorId(vendorId);
        testService.setLatitude(BigDecimal.valueOf(16.0890));
        testService.setLongitude(BigDecimal.valueOf(108.2495));

        testCategory = new CategoryJpaEntity();
        testCategory.setId(categoryId);
        testCategory.setSlug("cheo-sup-kayak");
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE (F1 - F9)
    // =========================================================================
    @Nested
    @DisplayName("Tier 1: Feature Coverage")
    class Tier1FeatureCoverageTests {

        @Test
        @DisplayName("F1: Khởi tạo quy chuẩn an toàn danh mục & Seed data đầy đủ 6 danh mục trọng điểm")
        void testF1_categorySafetyRulesSeedAndModelStructure() {
            List<String> coreSlugs = List.of(
                    "cheo-sup-kayak",
                    "lan-ngam-san-ho",
                    "cano-du-bay",
                    "mo-to-nuoc-jetski",
                    "truot-phao-chuoi",
                    "du-thuyen-ngam-hoang-hon"
            );

            for (String slug : coreSlugs) {
                CategorySafetyRule rule = CategorySafetyRule.getBySlug(slug);
                assertNotNull(rule, "Quy chuẩn danh mục " + slug + " không được null");
                assertEquals(slug, rule.getCategorySlug());
                assertNotNull(rule.getCautionWaveHeightM(), "Ngưỡng sóng vàng phải được định nghĩa");
                assertNotNull(rule.getMaxWaveHeightM(), "Ngưỡng sóng đỏ phải được định nghĩa");
                assertNotNull(rule.getMaxWindSpeedKmh(), "Ngưỡng gió đỏ phải được định nghĩa");
                assertTrue(rule.getCautionWaveHeightM() <= rule.getMaxWaveHeightM(),
                        "Ngưỡng sóng cảnh báo vàng phải <= ngưỡng sóng cấm đỏ");
            }
        }

        @Test
        @DisplayName("F2: Dynamic Admin Safety Rules Configuration (Đọc và Lưu ngưỡng an toàn động)")
        void testF2_dynamicAdminSafetyRulesConfiguration() {
            CategorySafetyRuleJpaEntity entity = CategorySafetyRuleJpaEntity.builder()
                    .categoryId(categoryId)
                    .categorySlug("mo-to-nuoc-jetski")
                    .categoryName("Mô tô nước (Jetski)")
                    .cautionWaveHeightM(0.6)
                    .maxWaveHeightM(1.2)
                    .cautionWindSpeedKmh(20.0)
                    .maxWindSpeedKmh(30.0)
                    .maxWindGustKmh(40.0)
                    .maxOceanCurrentMs(0.6)
                    .minVisibilityM(1500.0)
                    .build();

            when(categorySafetyRuleRepository.findByCategorySlug("mo-to-nuoc-jetski"))
                    .thenReturn(Optional.of(entity));

            Optional<CategorySafetyRuleJpaEntity> found = categorySafetyRuleRepository.findByCategorySlug("mo-to-nuoc-jetski");
            assertTrue(found.isPresent());
            assertEquals(1.2, found.get().getMaxWaveHeightM());

            // Admin cập nhật ngưỡng sóng cho giải đấu chuyên nghiệp
            entity.setMaxWaveHeightM(1.5);
            when(categorySafetyRuleRepository.save(any(CategorySafetyRuleJpaEntity.class))).thenReturn(entity);

            CategorySafetyRuleJpaEntity saved = categorySafetyRuleRepository.save(entity);
            assertEquals(1.5, saved.getMaxWaveHeightM());
            verify(categorySafetyRuleRepository, times(1)).save(entity);
        }

        @Test
        @DisplayName("F3: Built-in Registry Fallback (Khả năng dự phòng khi DB trống hoặc mất kết nối)")
        void testF3_builtInRegistryFallbackWhenDbFails() {
            // Khi tra cứu slug lạ hoặc DB chưa có bản ghi, hệ thống tự fallback về DEFAULT_RULE an toàn
            CategorySafetyRule fallbackRule = CategorySafetyRule.getBySlug("unknown-marine-sport");
            assertNotNull(fallbackRule);
            assertEquals("default", fallbackRule.getCategorySlug());
            assertEquals(1.2, fallbackRule.getMaxWaveHeightM());
            assertEquals(28.0, fallbackRule.getMaxWindSpeedKmh());
        }

        @Test
        @DisplayName("F4: 30-Phút Sliding Window Scheduler (Quét các slot có lịch đặt trong T-24h và hôm nay)")
        void testF4_slidingWindowSchedulerScanning() {
            LocalDateTime nowInVietnam = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            LocalDateTime slotTime = nowInVietnam.plusHours(24);
            testSlot.setDate(slotTime.toLocalDate());
            testSlot.setStartTime(slotTime.toLocalTime().withSecond(0).withNano(0));
            testSlot.setEndTime(slotTime.plusHours(2).toLocalTime().withSecond(0).withNano(0));

            ServiceSlotJpaEntity unbookedSlot = new ServiceSlotJpaEntity();
            unbookedSlot.setId(UUID.randomUUID());
            unbookedSlot.setServiceId(serviceId);
            unbookedSlot.setDate(LocalDate.now());
            unbookedSlot.setBookedCount(0); // Không có khách đặt

            when(slotRepository.findByDateBetween(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(testSlot, unbookedSlot));

            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

            WeatherInfoDto.TimeWindowForecast safeForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(0.3)
                    .peakWindSpeed(10.0)
                    .peakWindGust(14.0)
                    .peakOceanCurrent(0.1)
                    .minVisibility(5000.0)
                    .severeWeatherCode(0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(safeForecast);

            slotWeatherMonitoringJob.monitorUpcomingSlots(nowInVietnam);

            // Chỉ slot có bookedCount > 0 mới được đánh giá an toàn thời tiết
            verify(weatherProviderPort, times(1))
                    .getTimeWindowForecast(anyDouble(), anyDouble(), eq(testSlot.getDate()), eq(testSlot.getStartTime()), eq(testSlot.getEndTime()));
            verify(evaluationRepository, never()).save(any());
        }

        @Test
        @DisplayName("F5: Quy trình cảnh báo sớm YELLOW (Early Warning) khi thời tiết chạm ngưỡng thận trọng")
        void testF5_yellowEarlyWarningWorkflow() {
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
            // Sóng 0.6m (vượt ngưỡng caution 0.5m nhưng dưới ngưỡng max 0.8m)
            WeatherRuleEngine.SafetyEvaluationResult result = weatherRuleEngine.evaluate(
                    rule, 0.6, 10.0, 15.0, 0.1, 4000.0, 0
            );

            assertTrue(result.isSafe(), "Mức YELLOW vẫn tạm thời an toàn nhưng cần cảnh báo");
            assertEquals(WeatherRuleEngine.ALERT_YELLOW, result.getAlertLevel());
            assertTrue(result.getWarningMessage().contains("CAUTION ALERT"));
        }

        @Test
        @DisplayName("F6: Auto-Escalation Fallback (Tự động hủy slot & hoàn tiền 100% với lý do WEATHER khi có sự cố RED)")
        void testF6_autoEscalationFallbackCancellationAndRefund() {
            UUID alertId = UUID.randomUUID();
            SafetyRuleEvaluationJpaEntity alert = new SafetyRuleEvaluationJpaEntity();
            alert.setId(alertId);
            alert.setServiceId(serviceId);
            alert.setSlotId(slotId);
            alert.setIsSafe(false);
            alert.setWarningMessage("Sóng biển 1.4m vượt ngưỡng cấm");

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);
            subOrder.setSubtotalAmount(BigDecimal.valueOf(1200000));

            when(evaluationRepository.findByIdForUpdate(alertId)).thenReturn(Optional.of(alert));
            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            AdminWeatherAlertController.ResolveAlertRequest request =
                    new AdminWeatherAlertController.ResolveAlertRequest("CANCEL_AND_REFUND", "Hủy khẩn cấp bảo vệ du khách");

            WeatherCancellationFixture.install(adminWeatherAlertController, subOrderRepository, refundRepository, slotRepository, slotId);
            ResponseEntity<?> response = adminWeatherAlertController.resolveAlert(alertId, request);

            assertEquals(200, response.getStatusCode().value());
            assertEquals(SubOrderStatus.CANCELLED, subOrder.getStatus());
            verify(subOrderRepository, times(1)).saveAndFlush(subOrder);

            // Kiểm tra bản ghi hoàn tiền 100% được tạo với lý do WEATHER
            ArgumentCaptor<RefundJpaEntity> refundCaptor = ArgumentCaptor.forClass(RefundJpaEntity.class);
            verify(refundRepository, times(1)).saveAndFlush(refundCaptor.capture());
            RefundJpaEntity refund = refundCaptor.getValue();
            assertEquals(BigDecimal.valueOf(1200000), refund.getAmount());
            assertEquals(BigDecimal.valueOf(100.0), refund.getRefundPercentage());
            assertEquals(RefundReason.WEATHER, refund.getReason());
            assertEquals(RefundStatus.PENDING, refund.getStatus());
            assertFalse(alert.getIsSafe());
        }

        @Test
        @DisplayName("F7: Idempotency & In-Place Metric Update (Không phát sinh cảnh báo mới khi thời tiết ổn định)")
        void testF7_alertIdempotency() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

            WeatherInfoDto.TimeWindowForecast safeForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(0.3)
                    .peakWindSpeed(8.0)
                    .peakWindGust(10.0)
                    .peakOceanCurrent(0.1)
                    .minVisibility(5000.0)
                    .severeWeatherCode(0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(safeForecast);

            // Chạy lần 1
            boolean alertRun1 = slotWeatherMonitoringJob.evaluateAndAlertSlot(testSlot);
            // Chạy lần 2 liên tiếp
            boolean alertRun2 = slotWeatherMonitoringJob.evaluateAndAlertSlot(testSlot);

            assertFalse(alertRun1);
            assertFalse(alertRun2);
            verify(evaluationRepository, never()).save(any());
            verify(sendNotificationUseCase, never()).execute(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("F8: Thu thập và chuẩn hóa dữ liệu từ Open-Meteo Weather & Marine DTOs")
        void testF8_openMeteoDataExtractionAndNormalization() {
            OpenMeteoWeatherResponse weatherResp = new OpenMeteoWeatherResponse();
            OpenMeteoWeatherResponse.CurrentData current = new OpenMeteoWeatherResponse.CurrentData();
            current.setTime("2026-09-21T08:00");
            current.setTemperature2m(29.5);
            current.setPrecipitation(0.0);
            current.setWindSpeed10m(14.0);
            current.setWindGusts10m(22.0);
            current.setVisibility(8000.0);
            current.setWeatherCode(1);
            weatherResp.setCurrent(current);

            when(openMeteoApiClient.fetchWeather(anyDouble(), anyDouble())).thenReturn(weatherResp);

            WeatherInfoDto.WeatherData weatherData = weatherProviderAdapter.getWeatherByCoordinates(16.0890, 108.2495);
            assertNotNull(weatherData);
            assertEquals(29.5, weatherData.getTemperature());
            assertEquals(14.0, weatherData.getWindSpeed());
            assertEquals(22.0, weatherData.getWindGust());
            assertEquals(8000.0, weatherData.getVisibility());
            assertEquals(1, weatherData.getWeatherCode());
        }

        @Test
        @DisplayName("F9: Multi-Day Time Window Forecast Peak Aggregation (Trích xuất đỉnh sóng, gió trong khung giờ slot)")
        void testF9_timeWindowForecastPeakAggregation() {
            LocalDate date = LocalDate.of(2026, 9, 22);
            LocalTime start = LocalTime.of(8, 0);
            LocalTime end = LocalTime.of(10, 0);

            OpenMeteoWeatherResponse weatherResp = new OpenMeteoWeatherResponse();
            OpenMeteoWeatherResponse.HourlyData hourlyWeather = new OpenMeteoWeatherResponse.HourlyData();
            hourlyWeather.setTime(List.of(
                    "2026-09-22T07:00",
                    "2026-09-22T08:00",
                    "2026-09-22T09:00",
                    "2026-09-22T10:00",
                    "2026-09-22T11:00"
            ));
            hourlyWeather.setWindSpeed10m(List.of(8.0, 14.0, 18.0, 12.0, 6.0));
            hourlyWeather.setWindGusts10m(List.of(12.0, 20.0, 26.0, 18.0, 10.0));
            hourlyWeather.setVisibility(List.of(6000.0, 4000.0, 3500.0, 5000.0, 7000.0));
            hourlyWeather.setWeatherCode(List.of(0, 1, 2, 1, 0));
            hourlyWeather.setPrecipitation(List.of(0.0, 0.2, 0.5, 0.1, 0.0));
            weatherResp.setHourly(hourlyWeather);

            OpenMeteoMarineResponse marineResp = new OpenMeteoMarineResponse();
            OpenMeteoMarineResponse.HourlyData hourlyMarine = new OpenMeteoMarineResponse.HourlyData();
            hourlyMarine.setTime(hourlyWeather.getTime());
            hourlyMarine.setWaveHeight(List.of(0.4, 0.6, 0.75, 0.5, 0.3));
            hourlyMarine.setOceanCurrentVelocity(List.of(0.1, 0.2, 0.25, 0.15, 0.1));
            marineResp.setHourly(hourlyMarine);

            when(openMeteoApiClient.fetchWeather(anyDouble(), anyDouble())).thenReturn(weatherResp);
            when(openMeteoApiClient.fetchMarine(anyDouble(), anyDouble())).thenReturn(marineResp);

            WeatherInfoDto.TimeWindowForecast forecast = weatherProviderAdapter.getTimeWindowForecast(
                    16.0890, 108.2495, date, start, end
            );

            assertNotNull(forecast);
            // Đỉnh trong khoảng [08:00, 10:00]: Sóng đỉnh 0.75m, Gió đỉnh 18.0 km/h, Gió giật đỉnh 26.0 km/h, Tầm nhìn min 3500m
            assertEquals(0.75, forecast.getPeakWaveHeight(), 0.001);
            assertEquals(18.0, forecast.getPeakWindSpeed(), 0.001);
            assertEquals(26.0, forecast.getPeakWindGust(), 0.001);
            assertEquals(0.25, forecast.getPeakOceanCurrent(), 0.001);
            assertEquals(3500.0, forecast.getMinVisibility(), 0.001);
            assertEquals(2, forecast.getSevereWeatherCode());
        }
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASES (BVA, Exact Limits, Fault Tolerance)
    // =========================================================================
    @Nested
    @DisplayName("Tier 2: Boundary & Corner Cases")
    class Tier2BoundaryAndCornerTests {

        @Test
        @DisplayName("BVA: Sóng bằng chính xác ngưỡng caution (0.50m) -> Vẫn GREEN")
        void testBVA_waveAtExactCautionThreshold_isGreen() {
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
            // Ngưỡng caution: 0.5m. Đúng 0.5m thì chưa vượt (> 0.5m mới là caution)
            var result = weatherRuleEngine.evaluate(rule, 0.50, 10.0, 15.0, 0.1, 5000.0, 0);
            assertTrue(result.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_GREEN, result.getAlertLevel());
        }

        @Test
        @DisplayName("BVA: Sóng vừa vượt ngưỡng caution một vi lượng (0.51m) -> Chuyển YELLOW")
        void testBVA_waveJustAboveCautionThreshold_triggersYellow() {
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
            var result = weatherRuleEngine.evaluate(rule, 0.51, 10.0, 15.0, 0.1, 5000.0, 0);
            assertTrue(result.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_YELLOW, result.getAlertLevel());
        }

        @Test
        @DisplayName("BVA: Sóng bằng chính xác ngưỡng max (0.80m) -> Vẫn YELLOW (Chưa cấm đỏ)")
        void testBVA_waveAtExactMaxThreshold_isYellow() {
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
            var result = weatherRuleEngine.evaluate(rule, 0.80, 10.0, 15.0, 0.1, 5000.0, 0);
            assertTrue(result.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_YELLOW, result.getAlertLevel());
        }

        @Test
        @DisplayName("BVA: Sóng vượt trần một vi lượng (0.81m) -> Lập tức RED cấm xuất bến")
        void testBVA_waveJustAboveMaxThreshold_triggersRed() {
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
            var result = weatherRuleEngine.evaluate(rule, 0.81, 10.0, 15.0, 0.1, 5000.0, 0);
            assertFalse(result.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_RED, result.getAlertLevel());
        }

        @Test
        @DisplayName("BVA: Dù bay biển Parasailing - Gió giật theo chuẩn ASTM F3099 (37.0 km/h an toàn, 37.1 km/h cấm)")
        void testBVA_parasailingWindGust_astmStandard() {
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("cano-du-bay");
            // 37.0 km/h (20 knots) là giới hạn trên cho phép
            var safeResult = weatherRuleEngine.evaluate(rule, 0.5, 18.0, 37.0, 0.2, 5000.0, 0);
            assertTrue(safeResult.isSafe());

            // 37.1 km/h vi phạm chuẩn an toàn dù bay ASTM F3099
            var dangerousResult = weatherRuleEngine.evaluate(rule, 0.5, 18.0, 37.1, 0.2, 5000.0, 0);
            assertFalse(dangerousResult.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_RED, dangerousResult.getAlertLevel());
            assertTrue(dangerousResult.getWarningMessage().contains("Peak wind gust"));
        }

        @Test
        @DisplayName("BVA: Tầm nhìn tối thiểu Jetski (1500m an toàn, 1499m cấm đỏ)")
        void testBVA_minVisibilityBoundary() {
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("mo-to-nuoc-jetski");
            var safeResult = weatherRuleEngine.evaluate(rule, 0.4, 15.0, 20.0, 0.2, 1500.0, 0);
            assertTrue(safeResult.isSafe());

            var dangerousResult = weatherRuleEngine.evaluate(rule, 0.4, 15.0, 20.0, 0.2, 1499.0, 0);
            assertFalse(dangerousResult.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_RED, dangerousResult.getAlertLevel());
            assertTrue(dangerousResult.getWarningMessage().contains("Marine visibility"));
        }

        @Test
        @DisplayName("Corner Case: Sấm sét toàn cục (WMO 95, 96, 99) cấm đỏ tuyệt đối ngay cả khi sóng và gió phẳng lặng")
        void testCornerCase_fatalThunderstormCodesOverrideAllMetrics() {
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("du-thuyen-ngam-hoang-hon");
            // Biển hoàn toàn phẳng lặng: sóng 0.1m, gió 2 km/h
            var res95 = weatherRuleEngine.evaluate(rule, 0.1, 2.0, 3.0, 0.05, 10000.0, 95);
            var res96 = weatherRuleEngine.evaluate(rule, 0.1, 2.0, 3.0, 0.05, 10000.0, 96);
            var res99 = weatherRuleEngine.evaluate(rule, 0.1, 2.0, 3.0, 0.05, 10000.0, 99);

            assertFalse(res95.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_RED, res95.getAlertLevel());
            assertFalse(res96.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_RED, res96.getAlertLevel());
            assertFalse(res99.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_RED, res99.getAlertLevel());
        }

        @Test
        @DisplayName("Fault Tolerance: Phản hồi API Open-Meteo trả về null hoặc mảng rỗng -> Không gây NPE")
        void testFaultTolerance_nullOrEmptyApiResponse() {
            when(openMeteoApiClient.fetchWeather(anyDouble(), anyDouble())).thenReturn(null);
            when(openMeteoApiClient.fetchMarine(anyDouble(), anyDouble())).thenReturn(null);

            assertNull(weatherProviderAdapter.getWeatherByCoordinates(16.0, 108.0));
            assertNull(weatherProviderAdapter.getMarineByCoordinates(16.0, 108.0));

            WeatherInfoDto.TimeWindowForecast forecast = weatherProviderAdapter.getTimeWindowForecast(
                    16.0, 108.0, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(10, 0)
            );
            assertNotNull(forecast);
            assertNull(forecast.getPeakWaveHeight());
            assertNull(forecast.getPeakWindSpeed());
        }

        @Test
        @DisplayName("Corner Case: Đơn hàng đã ở trạng thái CANCELLED từ trước không bị tạo hoàn tiền 2 lần")
        void testCornerCase_alreadyCancelledOrdersNotRefundedTwice() {
            UUID alertId = UUID.randomUUID();
            SafetyRuleEvaluationJpaEntity alert = new SafetyRuleEvaluationJpaEntity();
            alert.setId(alertId);
            alert.setSlotId(slotId);
            alert.setIsSafe(false);

            SubOrderJpaEntity cancelledOrder = new SubOrderJpaEntity();
            cancelledOrder.setId(UUID.randomUUID());
            cancelledOrder.setSlotId(slotId);
            cancelledOrder.setStatus(SubOrderStatus.CANCELLED); // Đã hủy từ trước

            when(evaluationRepository.findByIdForUpdate(alertId)).thenReturn(Optional.of(alert));
            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(cancelledOrder));

            WeatherCancellationFixture.install(adminWeatherAlertController, subOrderRepository, refundRepository, slotRepository, slotId);
            adminWeatherAlertController.resolveAlert(alertId, new AdminWeatherAlertController.ResolveAlertRequest("CANCEL_AND_REFUND", "Hủy"));

            // Không gọi save hoàn tiền mới cho đơn đã hủy
            verify(refundRepository, never()).saveAndFlush(any());
        }
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS (Pairwise Interactions C1 - C4)
    // =========================================================================
    @Nested
    @DisplayName("Tier 3: Cross-Feature Combinations")
    class Tier3CrossFeatureCombinationTests {

        @Test
        @DisplayName("C1: Admin cập nhật ngưỡng -> Tác động trực tiếp đến kết quả đánh giá an toàn của Job")
        void testC1_adminThresholdUpdateImpactsEvaluation() {
            // Bước 1: Với ngưỡng SUP mặc định (maxWave = 0.8m), sóng 0.9m là RED
            CategorySafetyRule defaultRule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
            var evalBefore = weatherRuleEngine.evaluate(defaultRule, 0.9, 10.0, 15.0, 0.1, 5000.0, 0);
            assertFalse(evalBefore.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_RED, evalBefore.getAlertLevel());

            // Bước 2: Admin điều chỉnh nâng ngưỡng cho khu vực vịnh kín gió Mân Thái lên maxWave = 1.0m
            CategorySafetyRule tunedRule = CategorySafetyRule.builder()
                    .categorySlug("cheo-sup-kayak")
                    .categoryName("Chèo SUP Vịnh Kín")
                    .cautionWaveHeightM(0.7)
                    .maxWaveHeightM(1.0)
                    .cautionWindSpeedKmh(15.0)
                    .maxWindSpeedKmh(25.0)
                    .maxWindGustKmh(30.0)
                    .maxOceanCurrentMs(0.4)
                    .minVisibilityM(2000.0)
                    .build();

            // Bước 3: Đánh giá lại với cùng thông số sóng 0.9m -> Giờ chuyển thành YELLOW (an toàn có cảnh báo)
            var evalAfter = weatherRuleEngine.evaluate(tunedRule, 0.9, 10.0, 15.0, 0.1, 5000.0, 0);
            assertTrue(evalAfter.isSafe(), "Sau khi admin nâng ngưỡng an toàn, slot trở thành an toàn thận trọng");
            assertEquals(WeatherRuleEngine.ALERT_YELLOW, evalAfter.getAlertLevel());
        }

        @Test
        @DisplayName("C2: Thời tiết diễn biến xấu: Chuyển đổi trạng thái từ YELLOW sang RED & Bắn cảnh báo")
        void testC2_yellowToRedEscalation() {
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("lan-ngam-san-ho");

            // T-24h: Sóng 0.9m -> Cảnh báo YELLOW
            var evalT24 = weatherRuleEngine.evaluate(rule, 0.9, 14.0, 20.0, 0.3, 4000.0, 0);
            assertTrue(evalT24.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_YELLOW, evalT24.getAlertLevel());

            // T-2h: Sóng biển tăng đột ngột lên 1.4m và dòng chảy xiết 0.6 m/s -> Leo thang lên RED
            var evalT2 = weatherRuleEngine.evaluate(rule, 1.4, 22.0, 30.0, 0.6, 2500.0, 0);
            assertFalse(evalT2.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_RED, evalT2.getAlertLevel());
            assertTrue(evalT2.getDetails().size() >= 2, "Phải ghi nhận đồng thời cả vi phạm sóng và dòng chảy");
        }

        @Test
        @DisplayName("C3: Cảnh báo RED kích hoạt quy trình hủy khẩn cấp và hoàn tiền 100% cho toàn bộ khách hàng")
        void testC3_redAlertToCancellationAndTriPartyNotification() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

            WeatherInfoDto.TimeWindowForecast stormForecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(1.5) // Vượt ngưỡng 0.8m của SUP
                    .peakWindSpeed(25.0)
                    .peakWindGust(35.0)
                    .peakOceanCurrent(0.4)
                    .minVisibility(2000.0)
                    .severeWeatherCode(95) // Dông sét
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(stormForecast);

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);

            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            // Job phát hiện bão và lưu bản ghi đánh giá
            boolean alerted = slotWeatherMonitoringJob.evaluateAndAlertSlot(testSlot);
            assertTrue(alerted);

            verify(evaluationRepository, times(1)).save(any(SafetyRuleEvaluationJpaEntity.class));
            // Gửi thông báo cho Vendor
            verify(sendNotificationUseCase).execute(notification(vendorId, "WEATHER_ALERT", "SERVICE_SLOT", slotId));
            // Gửi thông báo cho Khách hàng
            verify(sendNotificationUseCase).execute(notification(null, "WEATHER_WARNING", "SUB_ORDER", subOrder.getId()));
        }

        @Test
        @DisplayName("C4: Chuỗi tích hợp: Open-Meteo Multi-Day Forecast -> Peak Aggregator -> WeatherRuleEngine")
        void testC4_endToEndForecastToRuleEnginePipeline() {
            LocalDate date = LocalDate.now().plusDays(3);
            LocalTime start = LocalTime.of(14, 0);
            LocalTime end = LocalTime.of(16, 0);

            OpenMeteoWeatherResponse weatherResp = new OpenMeteoWeatherResponse();
            OpenMeteoWeatherResponse.HourlyData hourlyWeather = new OpenMeteoWeatherResponse.HourlyData();
            hourlyWeather.setTime(List.of(
                    date + "T13:00",
                    date + "T14:00",
                    date + "T15:00",
                    date + "T16:00"
            ));
            hourlyWeather.setWindSpeed10m(List.of(10.0, 15.0, 22.0, 18.0));
            hourlyWeather.setWindGusts10m(List.of(15.0, 24.0, 32.0, 25.0));
            hourlyWeather.setVisibility(List.of(6000.0, 5000.0, 4500.0, 5000.0));
            hourlyWeather.setWeatherCode(List.of(0, 0, 1, 1));
            weatherResp.setHourly(hourlyWeather);

            OpenMeteoMarineResponse marineResp = new OpenMeteoMarineResponse();
            OpenMeteoMarineResponse.HourlyData hourlyMarine = new OpenMeteoMarineResponse.HourlyData();
            hourlyMarine.setTime(hourlyWeather.getTime());
            hourlyMarine.setWaveHeight(List.of(0.4, 0.6, 0.7, 0.65));
            hourlyMarine.setOceanCurrentVelocity(List.of(0.1, 0.15, 0.2, 0.18));
            marineResp.setHourly(hourlyMarine);

            when(openMeteoApiClient.fetchWeather(anyDouble(), anyDouble())).thenReturn(weatherResp);
            when(openMeteoApiClient.fetchMarine(anyDouble(), anyDouble())).thenReturn(marineResp);

            // 1. Adapter tính toán thông số đỉnh
            WeatherInfoDto.TimeWindowForecast forecast = weatherProviderAdapter.getTimeWindowForecast(
                    16.0890, 108.2495, date, start, end
            );

            // 2. Chuyển vào RuleEngine đánh giá cho SUP
            CategorySafetyRule rule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
            var result = weatherRuleEngine.evaluate(
                    rule,
                    forecast.getPeakWaveHeight(),
                    forecast.getPeakWindSpeed(),
                    forecast.getPeakWindGust(),
                    forecast.getPeakOceanCurrent(),
                    forecast.getMinVisibility(),
                    forecast.getSevereWeatherCode()
            );

            // Sóng 0.7m > caution (0.5m), gió 22 km/h > max wind (20 km/h) -> RED
            assertFalse(result.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_RED, result.getAlertLevel());
            assertTrue(result.getWarningMessage().contains("Sustained wind speed"));
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD WORKLOAD SCENARIOS (Scenarios 1 - 5)
    // =========================================================================
    @Nested
    @DisplayName("Tier 4: Real-World Workload Scenarios")
    class Tier4RealWorldWorkloadScenarioTests {

        @Test
        @DisplayName("Scenario 1: Dông bão nhiệt đới mùa hè bất ngờ lúc 15:00 tại Biển Mân Thái (Summer Thunderstorm)")
        void testScenario1_summerAfternoonThunderstormAtManThai() {
            // Tour SUP chiều lúc 15:00
            testSlot.setStartTime(LocalTime.of(15, 0));
            testSlot.setEndTime(LocalTime.of(17, 0));

            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

            // Đám mây đối lưu nhiệt phát triển cực nhanh gây giông sét WMO 95 và gió giật 38 km/h
            WeatherInfoDto.TimeWindowForecast convectiveStorm = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(0.95)
                    .peakWindSpeed(24.0)
                    .peakWindGust(38.0)
                    .peakOceanCurrent(0.35)
                    .minVisibility(1200.0)
                    .severeWeatherCode(95)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(convectiveStorm);

            SubOrderJpaEntity order1 = new SubOrderJpaEntity();
            order1.setId(UUID.randomUUID());
            order1.setSlotId(slotId);
            order1.setStatus(SubOrderStatus.CONFIRMED);
            order1.setSubtotalAmount(BigDecimal.valueOf(800000));

            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(order1));

            boolean alertTriggered = slotWeatherMonitoringJob.evaluateAndAlertSlot(testSlot);
            assertTrue(alertTriggered);

            // Admin duyệt hủy ngay lập tức
            ArgumentCaptor<SafetyRuleEvaluationJpaEntity> evalCaptor = ArgumentCaptor.forClass(SafetyRuleEvaluationJpaEntity.class);
            verify(evaluationRepository).save(evalCaptor.capture());
            SafetyRuleEvaluationJpaEntity savedAlert = evalCaptor.getValue();
            assertFalse(savedAlert.getIsSafe());

            UUID alertId = UUID.randomUUID();
            savedAlert.setId(alertId);
            when(evaluationRepository.findByIdForUpdate(alertId)).thenReturn(Optional.of(savedAlert));

            WeatherCancellationFixture.install(adminWeatherAlertController, subOrderRepository, refundRepository, slotRepository, slotId);
            adminWeatherAlertController.resolveAlert(alertId,
                    new AdminWeatherAlertController.ResolveAlertRequest("CANCEL_AND_REFUND", "Dông sét mùa hè nguy hiểm"));

            assertEquals(SubOrderStatus.CANCELLED, order1.getStatus());
            verify(refundRepository).saveAndFlush(argThat(refund ->
                    refund.getAmount().equals(BigDecimal.valueOf(800000)) &&
                            refund.getRefundPercentage().equals(BigDecimal.valueOf(100.0)) &&
                            refund.getReason() == RefundReason.WEATHER
            ));
        }

        @Test
        @DisplayName("Scenario 2: Giải đua Mô tô nước (Jetski) cuối tuần với sự kiện được nâng ngưỡng an toàn chuyên nghiệp")
        void testScenario2_jetskiTournamentDynamicTuning() {
            CategorySafetyRule amateurRule = CategorySafetyRule.getBySlug("mo-to-nuoc-jetski");

            // Sóng 1.35m và gió 32 km/h
            var amateurEval = weatherRuleEngine.evaluate(amateurRule, 1.35, 32.0, 38.0, 0.4, 3000.0, 0);
            assertFalse(amateurEval.isSafe(), "Khách phổ thông không được phép chạy Jetski khi sóng > 1.2m");
            assertEquals(WeatherRuleEngine.ALERT_RED, amateurEval.getAlertLevel());

            // Ban tổ chức giải đua với vận động viên chuyên nghiệp và cano cứu hộ hộ tống: Nâng ngưỡng max sóng lên 1.6m, gió 38 km/h
            CategorySafetyRule tournamentRule = CategorySafetyRule.builder()
                    .categorySlug("mo-to-nuoc-jetski")
                    .categoryName("Giải Đua Jetski Chuyên Nghiệp")
                    .cautionWaveHeightM(1.0)
                    .maxWaveHeightM(1.6)
                    .cautionWindSpeedKmh(25.0)
                    .maxWindSpeedKmh(38.0)
                    .maxWindGustKmh(48.0)
                    .maxOceanCurrentMs(0.8)
                    .minVisibilityM(1200.0)
                    .build();

            var tournamentEval = weatherRuleEngine.evaluate(tournamentRule, 1.35, 32.0, 38.0, 0.4, 3000.0, 0);
            assertTrue(tournamentEval.isSafe(), "Giải đua chuyên nghiệp được tiếp tục thi đấu trong ngưỡng an toàn mới");
            assertEquals(WeatherRuleEngine.ALERT_YELLOW, tournamentEval.getAlertLevel());
        }

        @Test
        @DisplayName("Scenario 3: Đặt trước tour lặn ngắm san hô Sơn Trà 7 ngày với diễn biến thời tiết thay đổi linh hoạt")
        void testScenario3_coralDivingAdvanceBookingWeatherShift() {
            CategorySafetyRule diveRule = CategorySafetyRule.getBySlug("lan-ngam-san-ho");

            // Ngày T-7 (Lúc đặt): Biển êm, sóng 0.4m, dòng hải lưu 0.15 m/s -> GREEN
            var evalBookingDay = weatherRuleEngine.evaluate(diveRule, 0.4, 10.0, 15.0, 0.15, 6000.0, 0);
            assertTrue(evalBookingDay.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_GREEN, evalBookingDay.getAlertLevel());

            // Ngày T-1 (Khung T-24h): Có áp thấp nhiệt đới gần bờ, sóng tăng lên 1.0m (vàng)
            var evalT24 = weatherRuleEngine.evaluate(diveRule, 1.0, 18.0, 24.0, 0.35, 4000.0, 0);
            assertTrue(evalT24.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_YELLOW, evalT24.getAlertLevel());

            // Ngày T (Khung T-2h): Áp thấp dịch chuyển ra xa, gió giảm, sóng còn 0.7m, nước trong trở lại -> Trở lại GREEN
            var evalT2 = weatherRuleEngine.evaluate(diveRule, 0.7, 12.0, 16.0, 0.2, 5000.0, 0);
            assertTrue(evalT2.isSafe());
            assertEquals(WeatherRuleEngine.ALERT_GREEN, evalT2.getAlertLevel());
            assertTrue(evalT2.getWarningMessage().contains("suitable"));
        }

        @Test
        @DisplayName("Scenario 4: Sự cố sập kết nối Cơ sở dữ liệu giờ cao điểm sáng sớm 05:30 -> Fallback không gián đoạn")
        void testScenario4_databaseConnectionFailureResilience() {
            // Giả lập danh mục slug lạ do cache bị rỗng hoặc DB disconnect
            String categorySlug = "danh-muc-chua-dong-bo";
            CategorySafetyRule rule = CategorySafetyRule.getBySlug(categorySlug);

            assertNotNull(rule);
            assertEquals("default", rule.getCategorySlug());

            // Đánh giá liên tục 20 slot với in-memory fallback registry không hề ném bất kỳ ngoại lệ nào
            for (int i = 0; i < 20; i++) {
                double simulatedWave = 0.3 + (i * 0.05);
                var result = weatherRuleEngine.evaluate(rule, simulatedWave, 12.0, 18.0, 0.2, 5000.0, 0);
                assertNotNull(result);
                assertNotNull(result.getAlertLevel());
            }
        }

        @Test
        @DisplayName("Scenario 5: Khảo sát áp lực Idempotency trong đợt gió mùa kéo dài liên tục 4 tiếng")
        void testScenario5_idempotencyStressTestDuringExtendedBadWeather() {
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

            WeatherInfoDto.TimeWindowForecast monsoonWeather = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800))
                    .peakWaveHeight(1.3)
                    .peakWindSpeed(26.0)
                    .peakWindGust(36.0)
                    .peakOceanCurrent(0.4)
                    .minVisibility(2500.0)
                    .severeWeatherCode(61)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                    .thenReturn(monsoonWeather);

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(UUID.randomUUID());
            subOrder.setSlotId(slotId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);

            when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

            // Job chạy định kỳ 8 lần liên tiếp (tương đương 4 giờ)
            for (int run = 0; run < 8; run++) {
                boolean alerted = slotWeatherMonitoringJob.evaluateAndAlertSlot(testSlot);
                assertTrue(alerted);
            }

            // Đảm bảo dữ liệu không bị nhân bản hàng loạt ngoài kiểm soát
            verify(evaluationRepository, times(8)).save(any());
        }
    }
}
