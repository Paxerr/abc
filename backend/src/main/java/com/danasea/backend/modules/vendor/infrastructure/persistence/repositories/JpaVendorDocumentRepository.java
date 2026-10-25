package com.danasea.backend.modules.vendor.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorDocumentJpaEntity;
import com.danasea.backend.modules.vendor.domain.models.DocStatus;

@Repository
public interface JpaVendorDocumentRepository extends JpaRepository<VendorDocumentJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from VendorDocumentJpaEntity d where d.id = :documentId and d.vendorId = :vendorId")
    Optional<VendorDocumentJpaEntity> findForReview(UUID vendorId, UUID documentId);

    List<VendorDocumentJpaEntity> findByVendorId(UUID vendorId);

    List<VendorDocumentJpaEntity> findAllByVendorId(UUID vendorId);

    List<VendorDocumentJpaEntity> findByVendorIdAndStatus(UUID vendorId, DocStatus status);
}
