package com.danasea.backend.modules.order.presentation.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.danasea.backend.modules.order.application.usecases.ConfirmSubOrderRescheduleUseCase;
import com.danasea.backend.modules.order.application.usecases.CreateRescheduleProposalUseCase;
import com.danasea.backend.modules.order.application.usecases.GetRescheduleOptionsUseCase;
import com.danasea.backend.modules.order.presentation.dtos.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubOrderRescheduleController Unit Tests")
class SubOrderRescheduleControllerTest {

    @Mock private GetRescheduleOptionsUseCase getRescheduleOptionsUseCase;

    @Mock private CreateRescheduleProposalUseCase createRescheduleProposalUseCase;

    @Mock private ConfirmSubOrderRescheduleUseCase confirmSubOrderRescheduleUseCase;

    @InjectMocks private SubOrderRescheduleController controller;

    @Test
    @DisplayName(
            "GET /api/sub-orders/{id}/reschedule-options: Trả HTTP 200 OK cùng danh sách ca khả"
                    + " dụng")
    void getRescheduleOptions_returns200Ok() {
        UUID subOrderId = UUID.randomUUID();
        RescheduleOptionsSummaryResponse mockResponse =
                new RescheduleOptionsSummaryResponse(
                        subOrderId,
                        UUID.randomUUID(),
                        LocalDate.now().plusDays(1),
                        LocalTime.of(8, 0),
                        UUID.randomUUID(),
                        "Tour lặn san hô",
                        2,
                        "PER_PERSON",
                        1L,
                        true,
                        "WEATHER_ALERT",
                        List.of(), null, false, null);

        when(getRescheduleOptionsUseCase.execute(subOrderId)).thenReturn(mockResponse);

        ResponseEntity<RescheduleOptionsSummaryResponse> response =
                controller.getRescheduleOptions(subOrderId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(getRescheduleOptionsUseCase, times(1)).execute(subOrderId);
    }

    @Test
    @DisplayName(
            "POST /api/vendor/sub-orders/{id}/reschedule-proposals: Vendor gửi đề xuất đổi ca, trả"
                    + " HTTP 200 OK")
    void createProposal_returns200Ok() {
        UUID subOrderId = UUID.randomUUID();
        UUID proposedSlotId = UUID.randomUUID();
        CreateRescheduleProposalRequest request =
                new CreateRescheduleProposalRequest(
                        proposedSlotId,
                        "VENDOR_UNAVAILABLE",
                        "Tàu cano bảo trì",
                        OffsetDateTime.now().plusHours(12));

        RescheduleProposalResponse mockResponse =
                new RescheduleProposalResponse(
                        UUID.randomUUID(),
                        subOrderId,
                        UUID.randomUUID(),
                        proposedSlotId,
                        LocalDate.now().plusDays(2),
                        LocalTime.of(14, 0),
                        LocalTime.of(16, 0),
                        "Tàu cano bảo trì",
                        "VENDOR_UNAVAILABLE",
                        "PENDING",
                        1L,
                        OffsetDateTime.now().plusHours(12),
                        OffsetDateTime.now());

        when(createRescheduleProposalUseCase.execute(subOrderId, request)).thenReturn(mockResponse);

        ResponseEntity<RescheduleProposalResponse> response =
                controller.createProposal(subOrderId, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(createRescheduleProposalUseCase, times(1)).execute(subOrderId, request);
    }

    @Test
    @DisplayName(
            "POST /api/sub-orders/{id}/reschedule: Khách xác nhận đổi ca với Idempotency-Key, trả"
                    + " HTTP 200 OK")
    void confirmReschedule_returns200Ok() {
        UUID subOrderId = UUID.randomUUID();
        UUID targetSlotId = UUID.randomUUID();
        String idempotencyKey = "key-test-abc";
        ConfirmRescheduleRequest request = new ConfirmRescheduleRequest(targetSlotId, null, 1L);

        SubOrderRescheduleResponse mockResponse =
                new SubOrderRescheduleResponse(
                        subOrderId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        targetSlotId,
                        LocalDate.now().plusDays(2),
                        LocalTime.of(9, 0),
                        2L,
                        BigDecimal.ZERO,
                        "RESCHEDULED",
                        OffsetDateTime.now(),
                        "Đổi lịch thành công.");

        when(confirmSubOrderRescheduleUseCase.execute(subOrderId, request, idempotencyKey))
                .thenReturn(mockResponse);

        ResponseEntity<SubOrderRescheduleResponse> response =
                controller.confirmReschedule(subOrderId, idempotencyKey, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(confirmSubOrderRescheduleUseCase, times(1))
                .execute(subOrderId, request, idempotencyKey);
    }
}
