package com.portfolio.payments.service;

import com.portfolio.payments.domain.PaymentTransaction;

public record PaymentServiceResult(
        PaymentTransaction transaction,
        boolean replayed
) {
}
