package com.portfolio.payments.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class PaymentRequestFingerprinter {

    public String fingerprint(PaymentCommand command) {
        String canonicalPayload = String.join("|",
                command.externalOrderId(),
                command.buyerLicense(),
                command.sellerLicense(),
                command.amount().toPlainString(),
                command.currency(),
                command.paymentMethodToken()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
