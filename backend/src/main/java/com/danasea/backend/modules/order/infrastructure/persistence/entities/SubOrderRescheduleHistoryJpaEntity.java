package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sub_order_reschedule_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubOrderRescheduleHistoryJpaEntity extends BaseJpaEntity {

    @Column(name = "sub_order_id", nullable = false)
    private UUID subOrderId;

    @Column(name = "booking_item_id", nullable = false)
    private UUID bookingItemId;

    @Column(name = "proposal_id")
    private UUID proposalId;

    @Column(name = "from_slot_id", nullable = false)
    private UUID fromSlotId;

    @Column(name = "to_slot_id", nullable = false)
    private UUID toSlotId;

    @Column(name = "from_booking_date", nullable = false)
    private LocalDate fromBookingDate;

    @Column(name = "from_booking_time", nullable = false)
    private LocalTime fromBookingTime;

    @Column(name = "to_booking_date", nullable = false)
    private LocalDate toBookingDate;

    @Column(name = "to_booking_time", nullable = false)
    private LocalTime toBookingTime;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "reason_type", nullable = false, length = 50)
    private String reasonType;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "reschedule_version", nullable = false, columnDefinition = "bigint default 1")
    private Long rescheduleVersion;

    @Column(name = "notification_sent_at")
    private OffsetDateTime notificationSentAt;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;
}
