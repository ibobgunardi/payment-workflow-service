package com.portfolio.payments.api.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        Map<String, String> details
) {

    public static ErrorResponse of(String code, String message, Instant timestamp) {
        return new ErrorResponse(code, message, timestamp, Map.of());
    }
}
