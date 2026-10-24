package com.danasea.backend.modules.vendor.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import com.danasea.backend.modules.vendor.domain.exceptions.InvalidVendorDocumentStateException;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VendorDocument extends BaseDomainModel {
    private UUID vendorId;
    private DocType docType;
    private String fileUrl;
    private DocStatus status;
    private UUID reviewedBy;
    private OffsetDateTime reviewedAt;

    public void review(DocStatus decision, UUID reviewerId, OffsetDateTime reviewedAt) {
        if (reviewerId == null || reviewedAt == null
                || (decision != DocStatus.APPROVED && decision != DocStatus.REJECTED)) {
            throw new IllegalArgumentException("A review requires an actor, time and final decision");
        }
        if (status != DocStatus.PENDING) {
            throw new InvalidVendorDocumentStateException();
        }
        this.status = decision;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
    }
}
