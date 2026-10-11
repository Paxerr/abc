package com.danasea.backend.modules.order.presentation.dtos;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderDisruptionJpaEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DisruptionResponse(
        UUID id,
        UUID subOrderId,
        RefundReason reason,
        String status,
        UUID originalSlotId,
        OffsetDateTime originalDepartureAt,
        OffsetDateTime decisionDeadline,
        UUID refundId,
        String details) {
    public static DisruptionResponse fromEntity(SubOrderDisruptionJpaEntity e) {
        return new DisruptionResponse(
                e.getId(),
                e.getSubOrderId(),
                e.getReason(),
                e.getStatus(),
                e.getOriginalSlotId(),
                e.getOriginalDepartureAt(),
                e.getDecisionDeadline(),
                e.getRefundId(),
                e.getDetails());
    }
}
