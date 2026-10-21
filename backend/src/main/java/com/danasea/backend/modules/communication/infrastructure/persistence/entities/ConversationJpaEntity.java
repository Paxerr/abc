package com.danasea.backend.modules.communication.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(name = "uq_conversations_participants_order", columnNames = {"customer_id", "vendor_id", "master_order_id"}))
public class ConversationJpaEntity extends BaseJpaEntity {

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "master_order_id")
    private UUID masterOrderId;

    @Column(name = "last_message_sequence", nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long lastMessageSequence = 0L;

}
