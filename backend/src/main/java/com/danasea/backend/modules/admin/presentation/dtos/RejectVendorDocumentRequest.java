package com.danasea.backend.modules.admin.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectVendorDocumentRequest(
        @NotBlank(message = "{validation.rejection_reason.required}") @Size(max = 2000) String reason) {
}
