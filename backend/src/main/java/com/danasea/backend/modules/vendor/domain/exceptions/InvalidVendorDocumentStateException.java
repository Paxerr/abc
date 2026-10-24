package com.danasea.backend.modules.vendor.domain.exceptions;

public class InvalidVendorDocumentStateException extends RuntimeException {
    public InvalidVendorDocumentStateException() {
        super("Only pending vendor documents can be reviewed");
    }
}
