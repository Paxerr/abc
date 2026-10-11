package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import com.danasea.backend.modules.order.infrastructure.persistence.entities.DiscountRedemptionJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaDiscountRedemptionRepository extends JpaRepository<DiscountRedemptionJpaEntity, UUID> {

    long countByDiscountCodeIdAndCustomerId(UUID discountCodeId, UUID customerId);

    List<DiscountRedemptionJpaEntity> findByMasterOrderId(UUID masterOrderId);

    Optional<DiscountRedemptionJpaEntity> findByMasterOrderIdAndDiscountCodeId(UUID masterOrderId, UUID discountCodeId);

    void deleteByMasterOrderId(UUID masterOrderId);

    @Modifying
    @Query("DELETE FROM DiscountRedemptionJpaEntity r WHERE r.masterOrderId = :orderId AND r.discountCodeId = :codeId "
            + "AND EXISTS (SELECT o.id FROM MasterOrderJpaEntity o WHERE o.id = :orderId "
            + "AND o.status = 'PENDING_PAYMENT' AND o.paymentStatus = 'UNPAID')")
    int releaseReservation(@Param("orderId") UUID orderId, @Param("codeId") UUID codeId);
}
