package com.danasea.backend.modules.vendor.domain.exceptions;

public class VendorDocumentNotFoundException extends RuntimeException {
    public VendorDocumentNotFoundException() {
        super("Document not found for the specified vendor");
    }
}
