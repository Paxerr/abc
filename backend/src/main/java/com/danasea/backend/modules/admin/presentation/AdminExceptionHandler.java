package com.danasea.backend.modules.admin.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasea.backend.modules.admin.domain.exceptions.SelfLockNotAllowedException;
import com.danasea.backend.modules.admin.domain.exceptions.UserAlreadyLockedException;
import com.danasea.backend.modules.admin.domain.exceptions.UserAlreadyUnlockedException;
import com.danasea.backend.modules.admin.domain.exceptions.UserNotFoundException;
import com.danasea.backend.modules.admin.domain.exceptions.AuditLogNotFoundException;
import com.danasea.backend.modules.admin.domain.exceptions.VendorDocumentsIncompleteException;
import com.danasea.backend.modules.vendor.domain.exceptions.VendorDocumentNotFoundException;
import com.danasea.backend.modules.vendor.domain.exceptions.InvalidVendorDocumentStateException;
import com.danasea.backend.modules.vendor.domain.exceptions.VendorNotFoundException;
import com.danasea.backend.security.authorization.domain.exceptions.AccessDeniedException;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.presentation.ErrorResponse;
import java.util.Locale;

@RestControllerAdvice
public class AdminExceptionHandler {
    private LocalizedMessageService messages = LocalizedMessageService.standalone();

    @ExceptionHandler(VendorDocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVendorDocumentNotFound(VendorDocumentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("VENDOR_DOCUMENT_NOT_FOUND"));
    }

    @ExceptionHandler(InvalidVendorDocumentStateException.class)
    public ResponseEntity<ErrorResponse> handleVendorDocumentState(InvalidVendorDocumentStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("INVALID_VENDOR_DOCUMENT_STATE"));
    }

    @ExceptionHandler(VendorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVendorNotFound(VendorNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("VENDOR_NOT_FOUND"));
    }

    @Autowired(required = false)
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.messages = messages;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error("USER_NOT_FOUND"));
    }

    @ExceptionHandler(AuditLogNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuditLogNotFound(AuditLogNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error("AUDIT_LOG_NOT_FOUND"));
    }

    @ExceptionHandler(VendorDocumentsIncompleteException.class)
    public ResponseEntity<ErrorResponse> handleVendorDocumentsIncomplete(VendorDocumentsIncompleteException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error("VENDOR_DOCUMENTS_INCOMPLETE"));
    }

    @ExceptionHandler(SelfLockNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleSelfLockNotAllowed(SelfLockNotAllowedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error("SELF_LOCK_NOT_ALLOWED"));
    }

    @ExceptionHandler(UserAlreadyLockedException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyLocked(UserAlreadyLockedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error("USER_ALREADY_LOCKED"));
    }

    @ExceptionHandler(UserAlreadyUnlockedException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyUnlocked(UserAlreadyUnlockedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error("USER_ALREADY_UNLOCKED"));
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            AuthorizationDeniedException.class,
            AccessDeniedException.class,
    })
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error("ACCESS_DENIED"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(messages.get("error.invalid_input"));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_INPUT", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error("BAD_REQUEST"));
    }

    private ErrorResponse error(String code) {
        String key = "error." + code.toLowerCase(Locale.ROOT);
        return new ErrorResponse(code, messages.getOrDefault(key, messages.get("error.internal")));
    }
}
