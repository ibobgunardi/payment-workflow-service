package com.portfolio.payments.service;

import java.math.BigDecimal;
import java.util.Locale;

public record PaymentCommand(
        String externalOrderId,
        String buyerLicense,
        String sellerLicense,
        BigDecimal amount,
        String currency,
        String paymentMethodToken
) {

    public PaymentCommand {
        externalOrderId = externalOrderId.trim();
        buyerLicense = buyerLicense.trim();
        sellerLicense = sellerLicense.trim();
        amount = amount.stripTrailingZeros();
        currency = currency.trim().toUpperCase(Locale.ROOT);
        paymentMethodToken = paymentMethodToken.trim();
    }
}
