package com.portfolio.payments.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentGatewayRequest(
        UUID paymentId,
        String externalOrderId,
        BigDecimal amount,
        String currency,
        String paymentMethodToken
) {
}
