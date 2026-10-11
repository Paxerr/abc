package com.danasea.backend.modules.order.application.dtos;

import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubOrderDetailResult(
        UUID id,
        UUID bookingItemId,
        UUID masterOrderId,
        UUID vendorId,
        UUID serviceId,
        UUID slotId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal vendorDiscountAmount,
        BigDecimal platformDiscountAmount,
        BigDecimal finalAmount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal vendorPayoutAmount,
        SubOrderStatus status,
        Boolean waiverRequired,
        Integer waiverVersion,
        String waiverContent,
        String waiverContentEn,
        Boolean waiverAccepted,
        OffsetDateTime waiverAcceptedAt,
        UUID waiverAcceptedBy,
        String waiverAcceptedLanguage,
        String waiverAcceptedContent,
        OffsetDateTime vendorNotifiedAt,
        OffsetDateTime createdAt,
        String cancellationPolicyVersion
) {
    public SubOrderDetailResult(
            UUID id,
            UUID bookingItemId,
            UUID masterOrderId,
            UUID vendorId,
            UUID serviceId,
            UUID slotId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal vendorDiscountAmount,
            BigDecimal platformDiscountAmount,
            BigDecimal finalAmount,
            BigDecimal commissionRate,
            BigDecimal commissionAmount,
            BigDecimal vendorPayoutAmount,
            SubOrderStatus status,
            Boolean waiverAccepted,
            OffsetDateTime vendorNotifiedAt,
            OffsetDateTime createdAt
    ) {
        this(id, bookingItemId, masterOrderId, vendorId, serviceId, slotId, quantity, unitPrice, subtotalAmount,
                discountAmount, vendorDiscountAmount, platformDiscountAmount, finalAmount,
                commissionRate, commissionAmount, vendorPayoutAmount, status,
                false, 1, null, null, waiverAccepted, null, null, null, null, vendorNotifiedAt, createdAt, "STANDARD_24H_V2");
    }

    public SubOrderDetailResult(
            UUID id,
            UUID bookingItemId,
            UUID masterOrderId,
            UUID vendorId,
            UUID serviceId,
            UUID slotId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotalAmount,
            BigDecimal commissionRate,
            BigDecimal commissionAmount,
            BigDecimal vendorPayoutAmount,
            SubOrderStatus status,
            Boolean waiverAccepted,
            OffsetDateTime vendorNotifiedAt,
            OffsetDateTime createdAt
    ) {
        this(id, bookingItemId, masterOrderId, vendorId, serviceId, slotId, quantity, unitPrice, subtotalAmount,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, subtotalAmount,
                commissionRate, commissionAmount, vendorPayoutAmount, status,
                false, 1, null, null, waiverAccepted, null, null, null, null, vendorNotifiedAt, createdAt, "STANDARD_24H_V2");
    }

    public static SubOrderDetailResult fromDomain(SubOrder subOrder) {
        if (subOrder == null) {
            return null;
        }
        return new SubOrderDetailResult(
                subOrder.getId(),
                subOrder.getBookingItemId(),
                subOrder.getMasterOrderId(),
                subOrder.getVendorId(),
                subOrder.getServiceId(),
                subOrder.getSlotId(),
                subOrder.getQuantity(),
                subOrder.getUnitPrice(),
                subOrder.getSubtotalAmount(),
                subOrder.getDiscountAmount() != null ? subOrder.getDiscountAmount() : BigDecimal.ZERO,
                subOrder.getVendorDiscountAmount() != null ? subOrder.getVendorDiscountAmount() : BigDecimal.ZERO,
                subOrder.getPlatformDiscountAmount() != null ? subOrder.getPlatformDiscountAmount() : BigDecimal.ZERO,
                subOrder.getFinalAmount(),
                subOrder.getCommissionRate(),
                subOrder.getCommissionAmount(),
                subOrder.getVendorPayoutAmount(),
                subOrder.getStatus(),
                Boolean.TRUE.equals(subOrder.getWaiverRequired()),
                subOrder.getWaiverVersion() != null ? subOrder.getWaiverVersion() : 1,
                subOrder.getWaiverContent(),
                subOrder.getWaiverContentEn(),
                Boolean.TRUE.equals(subOrder.getWaiverAccepted()),
                subOrder.getWaiverAcceptedAt(),
                subOrder.getWaiverAcceptedBy(),
                subOrder.getWaiverAcceptedLanguage(),
                subOrder.getWaiverAcceptedContent(),
                subOrder.getVendorNotifiedAt(),
                subOrder.getCreatedAt(),
                subOrder.getCancellationPolicyVersion()
        );
    }
}
