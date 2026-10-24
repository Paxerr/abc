package com.danasea.backend.modules.admin.application.usecases;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.exceptions.VendorDocumentNotFoundException;
import com.danasea.backend.modules.vendor.domain.exceptions.VendorNotFoundException;
import com.danasea.backend.modules.vendor.domain.models.DocStatus;
import com.danasea.backend.modules.vendor.domain.models.VendorDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewVendorDocumentUseCase {
    private final VendorInternalApi vendors;
    private final AuditLogInternalApi audit;
    private final Clock clock;

    public VendorDocument approve(UUID vendorId, UUID documentId, UUID adminId) {
        return review(vendorId, documentId, adminId, DocStatus.APPROVED, null);
    }

    public VendorDocument reject(UUID vendorId, UUID documentId, UUID adminId, String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 2000) {
            throw new IllegalArgumentException("Rejection reason is required and must not exceed 2000 characters");
        }
        return review(vendorId, documentId, adminId, DocStatus.REJECTED, reason.trim());
    }

    private VendorDocument review(UUID vendorId, UUID documentId, UUID adminId, DocStatus decision, String reason) {
        vendors.findById(vendorId).orElseThrow(VendorNotFoundException::new);
        VendorDocument document = vendors.findDocumentForReview(vendorId, documentId)
                .orElseThrow(VendorDocumentNotFoundException::new);
        document.review(decision, adminId, OffsetDateTime.now(clock));
        VendorDocument saved = vendors.saveDocument(document);
        // The existing audit table holds the reason atomically; vendor_documents has no reason column.
        audit.recordTransactionalAuditLog(adminId, "VENDOR_DOCUMENT_" + decision.name(),
                "VENDOR_DOCUMENT", documentId,
                "Vendor: " + vendorId + (reason == null ? "; Approved" : "; Rejection reason: " + reason));
        return saved;
    }
}
