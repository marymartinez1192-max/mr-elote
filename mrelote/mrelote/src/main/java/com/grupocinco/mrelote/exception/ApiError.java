package com.grupocinco.mrelote.exception;

import java.time.Instant;

public record ApiError(
        int status,
        String error,
        String message,
        String timestamp,
        String path
) {
    public ApiError(int status, String error, String message, String path) {
        this(status, error, message, Instant.now().toString(), path);
    }
}
