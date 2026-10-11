package com.danasea.backend.modules.order.presentation.controllers;

import com.danasea.backend.modules.order.application.services.DisruptionDecisionService;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.presentation.dtos.DisruptionResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DisruptionController {
    private final DisruptionDecisionService decisions;

    public record VerifyDisruptionRequest(
            @NotNull RefundReason reason, @NotBlank @Size(max = 2000) String details) {}

    @PostMapping("/api/admin/sub-orders/{id}/disruptions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record a verified disruption and offer rescheduling or a full refund")
    public ResponseEntity<DisruptionResponse> verify(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyDisruptionRequest request,
            @RequestHeader("Idempotency-Key") String key) {
        if (!key.matches("[A-Za-z0-9._:-]{8,100}"))
            throw new IllegalArgumentException("Invalid Idempotency-Key.");
        var disruption =
                decisions.record(
                        id,
                        request.reason(),
                        request.details().trim(),
                        "admin-" + key,
                        currentUser());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(DisruptionResponse.fromEntity(disruption));
    }

    @PostMapping("/api/sub-orders/{id}/disruption-refund")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Choose a full refund for an already verified disruption")
    public ResponseEntity<DisruptionResponse> refund(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(DisruptionResponse.fromEntity(decisions.chooseRefund(id, currentUser())));
    }

    private UUID currentUser() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated."));
    }
}
