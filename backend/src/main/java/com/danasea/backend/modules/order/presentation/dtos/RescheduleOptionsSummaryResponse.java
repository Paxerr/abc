package com.danasea.backend.modules.order.presentation.dtos;

import com.danasea.backend.modules.order.domain.models.RefundReason;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RescheduleOptionsSummaryResponse(
        UUID subOrderId,
        UUID currentSlotId,
        LocalDate currentBookingDate,
        LocalTime currentBookingTime,
        UUID serviceId,
        String serviceName,
        Integer quantity,
        String pricingUnit,
        Long rescheduleVersion,
        boolean rescheduleAllowed,
        String eligibilityReason,
        List<RescheduleOptionItemResponse> options,
        OffsetDateTime decisionDeadline,
        boolean fullRefundAvailable,
        RefundReason disruptionReason) {}
