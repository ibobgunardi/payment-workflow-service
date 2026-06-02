package com.portfolio.payments.service;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID paymentId) {
        super("payment not found: " + paymentId);
    }
}
