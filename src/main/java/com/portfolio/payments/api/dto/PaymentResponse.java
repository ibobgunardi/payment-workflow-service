package com.portfolio.payments.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.portfolio.payments.domain.PaymentTransaction;

public record PaymentResponse(
        UUID id,
        String externalOrderId,
        BigDecimal amount,
        String currency,
        String status,
        String providerTransactionId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        boolean replayed
) {

    public static PaymentResponse from(PaymentTransaction transaction, boolean replayed) {
        return new PaymentResponse(
                transaction.getId(),
                transaction.getExternalOrderId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus().name(),
                transaction.getProviderTransactionId(),
                transaction.getFailureReason(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                replayed
        );
    }
}
