package com.danasea.backend.modules.ai.infrastructure.cliproxy;

public class CliProxyException extends RuntimeException {
    public CliProxyException(String message) {
        super(message);
    }

    public CliProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
