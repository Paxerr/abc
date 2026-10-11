package com.danasea.backend.modules.order.presentation.dtos;

import com.danasea.backend.modules.order.domain.models.SubOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubOrderResponse(
        UUID id,
        UUID vendorId,
        UUID serviceId,
        UUID slotId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotalAmount,
        SubOrderStatus status,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        Boolean waiverRequired,
        Integer waiverVersion,
        String waiverContent,
        String waiverLanguage,
        Boolean waiverFallbackUsed,
        Boolean waiverAccepted,
        OffsetDateTime waiverAcceptedAt,
        UUID waiverAcceptedBy,
        String waiverAcceptedLanguage,
        String cancellationPolicyVersion
) {
    public SubOrderResponse(
            UUID id,
            UUID vendorId,
            UUID serviceId,
            UUID slotId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotalAmount,
            SubOrderStatus status,
            BigDecimal discountAmount,
            BigDecimal finalAmount
    ) {
        this(id, vendorId, serviceId, slotId, quantity, unitPrice, subtotalAmount, status,
                discountAmount, finalAmount, false, 1, null, null, false, false, null, null, null, "STANDARD_24H_V2");
    }

    public SubOrderResponse(
            UUID id,
            UUID vendorId,
            UUID serviceId,
            UUID slotId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotalAmount,
            SubOrderStatus status
    ) {
        this(id, vendorId, serviceId, slotId, quantity, unitPrice, subtotalAmount, status, BigDecimal.ZERO, subtotalAmount);
    }
}
