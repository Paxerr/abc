package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sub_order_disruptions")
@Getter
@Setter
public class SubOrderDisruptionJpaEntity extends BaseJpaEntity {
    private UUID subOrderId;
    private UUID originalSlotId;
    private OffsetDateTime originalDepartureAt;
    private OffsetDateTime decisionDeadline;
    @Enumerated(EnumType.STRING)
    private RefundReason reason;
    @Column(columnDefinition = "TEXT")
    private String details;
    @Column(length = 150)
    private String sourceKey;
    private UUID verifiedBy;
    @Column(length = 30)
    private String status;
    private UUID refundId;
    private OffsetDateTime notificationSentAt;
}
