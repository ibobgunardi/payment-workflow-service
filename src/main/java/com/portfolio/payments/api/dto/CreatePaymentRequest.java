package com.portfolio.payments.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.portfolio.payments.service.PaymentCommand;

public record CreatePaymentRequest(
        @NotBlank @Size(max = 128) String externalOrderId,
        @NotBlank @Size(max = 128) String buyerLicense,
        @NotBlank @Size(max = 128) String sellerLicense,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currency,
        @NotBlank @Size(max = 256) String paymentMethodToken
) {

    public PaymentCommand toCommand() {
        return new PaymentCommand(
                externalOrderId,
                buyerLicense,
                sellerLicense,
                amount,
                currency,
                paymentMethodToken
        );
    }
}
