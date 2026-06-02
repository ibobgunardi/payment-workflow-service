package com.portfolio.payments.domain;

public enum PaymentEventType {
    PAYMENT_CREATED,
    AUTHORIZATION_ATTEMPTED,
    PAYMENT_AUTHORIZED,
    PAYMENT_DECLINED,
    PAYMENT_FAILED
}
