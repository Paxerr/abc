package com.danasea.backend.modules.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;
import com.danasea.backend.modules.weather.presentation.controllers.AdminWeatherAlertController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminWeatherAlertController Tests")
class AdminWeatherAlertControllerTest {

    @Mock
    private JpaSafetyRuleEvaluationRepository evaluationRepository;

    @Mock
    private JpaSubOrderRepository subOrderRepository;

    @Mock
    private JpaRefundRepository refundRepository;

    @Mock
    private JpaServiceSlotRepository slotRepository;

    @Mock
    private JpaServiceRepository serviceRepository;

    @Mock
    private SendNotificationUseCase sendNotificationUseCase;

    private AdminWeatherAlertController controller;

    private UUID evaluationId;
    private UUID serviceId;
    private UUID slotId;
    private SafetyRuleEvaluationJpaEntity alertEntity;

    @BeforeEach
    void setUp() {
        controller = new AdminWeatherAlertController(
                evaluationRepository,
                subOrderRepository,
                refundRepository,
                slotRepository,
                serviceRepository,
                sendNotificationUseCase,
                new RefundPolicyEngine());
        evaluationId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
        slotId = UUID.randomUUID();

        alertEntity = new SafetyRuleEvaluationJpaEntity();
        alertEntity.setId(evaluationId);
        alertEntity.setServiceId(serviceId);
        alertEntity.setSlotId(slotId);
        alertEntity.setIsSafe(false);
        alertEntity.setStatus("AWAITING_ADMIN_RESOLUTION");
        alertEntity.setAlertLevel("RED");
        alertEntity.setWarningMessage("Sóng cao 1.5m vượt ngưỡng");
        alertEntity.setPeakWaveHeightM(1.5);
        alertEntity.setPeakWindSpeedKmh(25.0);
        alertEntity.setEvaluatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Admin lấy danh sách cảnh báo thời tiết active")
    void getActiveWeatherAlerts_success() {
        ServiceJpaEntity service = new ServiceJpaEntity();
        service.setId(serviceId);
        service.setName("Tour Lặn Ngắm San Hô Bán Đảo Sơn Trà");

        ServiceSlotJpaEntity slot = new ServiceSlotJpaEntity();
        slot.setId(slotId);
        slot.setDate(LocalDate.now().plusDays(1));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setBookedCount(5);

        when(evaluationRepository.findByStatusIn(anyList())).thenReturn(List.of(alertEntity));
        when(serviceRepository.findAllById(any())).thenReturn(List.of(service));
        when(slotRepository.findAllById(any())).thenReturn(List.of(slot));

        ResponseEntity<List<AdminWeatherAlertController.WeatherAlertResponse>> response = controller.getActiveWeatherAlerts();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        List<AdminWeatherAlertController.WeatherAlertResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals("Tour Lặn Ngắm San Hô Bán Đảo Sơn Trà", body.get(0).getServiceName());
        assertEquals(5, body.get(0).getAffectedBookingsCount());
        assertFalse(body.get(0).getIsSafe());
        assertEquals("AWAITING_ADMIN_RESOLUTION", body.get(0).getStatus());
        assertEquals("RED", body.get(0).getAlertLevel());
        assertEquals(1.5, body.get(0).getPeakWaveHeightM());
    }

    @Test
    @DisplayName("Admin lấy danh sách bao gồm cả MONITORING_YELLOW và AWAITING_ADMIN_RESOLUTION")
    void getActiveWeatherAlerts_includesBothMonitoringYellowAndAwaitingAdminResolution() {
        SafetyRuleEvaluationJpaEntity yellowAlert = new SafetyRuleEvaluationJpaEntity();
        yellowAlert.setId(UUID.randomUUID());
        yellowAlert.setServiceId(serviceId);
        yellowAlert.setSlotId(slotId);
        yellowAlert.setIsSafe(true);
        yellowAlert.setStatus("MONITORING_YELLOW");
        yellowAlert.setAlertLevel("YELLOW");
        yellowAlert.setWarningMessage("Sóng cao 0.6m thận trọng");
        yellowAlert.setPeakWaveHeightM(0.6);

        ServiceJpaEntity service = new ServiceJpaEntity();
        service.setId(serviceId);
        service.setName("Tour Chèo SUP");

        ServiceSlotJpaEntity slot = new ServiceSlotJpaEntity();
        slot.setId(slotId);
        slot.setDate(LocalDate.now().plusDays(1));
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setBookedCount(3);

        when(evaluationRepository.findByStatusIn(anyList())).thenReturn(List.of(alertEntity, yellowAlert));
        when(serviceRepository.findAllById(any())).thenReturn(List.of(service));
        when(slotRepository.findAllById(any())).thenReturn(List.of(slot));

        ResponseEntity<List<AdminWeatherAlertController.WeatherAlertResponse>> response = controller.getActiveWeatherAlerts();

        assertEquals(200, response.getStatusCode().value());
        List<AdminWeatherAlertController.WeatherAlertResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals("AWAITING_ADMIN_RESOLUTION", body.get(0).getStatus());
        assertEquals("MONITORING_YELLOW", body.get(1).getStatus());
        assertEquals("YELLOW", body.get(1).getAlertLevel());
    }

    @Test
    @DisplayName("Admin duyệt HỦY & HOÀN TIỀN 100% do thời tiết xấu")
    void resolveAlert_cancelAndRefund_success() {
        SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
        subOrder.setId(UUID.randomUUID());
        subOrder.setSlotId(slotId);
        subOrder.setStatus(SubOrderStatus.CONFIRMED);
        subOrder.setSubtotalAmount(BigDecimal.valueOf(1500000));
        subOrder.setFinalAmount(BigDecimal.valueOf(1200000));

        when(evaluationRepository.findByIdForUpdate(evaluationId)).thenReturn(Optional.of(alertEntity));
        when(subOrderRepository.findBySlotId(slotId)).thenReturn(List.of(subOrder));

        AdminWeatherAlertController.ResolveAlertRequest request =
                new AdminWeatherAlertController.ResolveAlertRequest("CANCEL_AND_REFUND", "Bão giật cấp 6 không an toàn");

        WeatherCancellationFixture.install(controller, subOrderRepository, refundRepository, slotRepository, slotId);
        ResponseEntity<Map<String, Object>> response = controller.resolveAlert(evaluationId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("RESOLVED_CANCEL_AND_REFUND", response.getBody().get("status"));
        assertEquals(1, response.getBody().get("refundedOrdersCount"));

        // Kiểm tra SubOrder chuyển trạng thái CANCELLED
        assertEquals(SubOrderStatus.CANCELLED, subOrder.getStatus());
        verify(subOrderRepository).saveAndFlush(subOrder);

        // Kiểm tra tạo bản ghi Refund 100% với lý do WEATHER
        ArgumentCaptor<RefundJpaEntity> refundCaptor = ArgumentCaptor.forClass(RefundJpaEntity.class);
        verify(refundRepository).saveAndFlush(refundCaptor.capture());
        RefundJpaEntity capturedRefund = refundCaptor.getValue();
        assertEquals(BigDecimal.valueOf(1200000), capturedRefund.getAmount());
        assertEquals(BigDecimal.valueOf(100.0), capturedRefund.getRefundPercentage());
        assertEquals(RefundReason.WEATHER, capturedRefund.getReason());
        assertEquals(RefundStatus.PENDING, capturedRefund.getStatus());
        assertNull(capturedRefund.getProcessedAt());

        // Kiểm tra cập nhật alertEntity
        assertFalse(alertEntity.getIsSafe());
        assertEquals("RESOLVED_CANCEL_AND_REFUND", alertEntity.getStatus());
        verify(evaluationRepository).save(alertEntity);
    }

    @Test
    @DisplayName("Admin duyệt MIỄN TRỪ / DỜI LỊCH (DISMISSED)")
    void resolveAlert_dismiss_success() {
        when(evaluationRepository.findByIdForUpdate(evaluationId)).thenReturn(Optional.of(alertEntity));

        AdminWeatherAlertController.ResolveAlertRequest request =
                new AdminWeatherAlertController.ResolveAlertRequest("DISMISSED", "Đã thỏa thuận dời sang buổi chiều an toàn");

        WeatherCancellationFixture.install(controller, subOrderRepository, refundRepository, slotRepository, slotId);
        ResponseEntity<Map<String, Object>> response = controller.resolveAlert(evaluationId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("RESOLVED_DISMISSED", response.getBody().get("status"));
        assertFalse(alertEntity.getIsSafe());
        assertEquals("RESOLVED_DISMISSED", alertEntity.getStatus());
        verify(evaluationRepository).save(alertEntity);
        verify(subOrderRepository, never()).saveAndFlush(any());
        verify(refundRepository, never()).saveAndFlush(any());
    }
}
