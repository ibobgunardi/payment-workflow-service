package com.portfolio.payments.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.payments.domain.PaymentEvent;
import com.portfolio.payments.domain.PaymentEventType;
import com.portfolio.payments.domain.PaymentTransaction;
import com.portfolio.payments.gateway.GatewayAuthorization;
import com.portfolio.payments.gateway.PaymentGatewayClient;
import com.portfolio.payments.gateway.PaymentGatewayRequest;
import com.portfolio.payments.gateway.TransientGatewayException;
import com.portfolio.payments.repository.PaymentEventRepository;
import com.portfolio.payments.repository.PaymentTransactionRepository;

@Service
public class PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentEventRepository eventRepository;
    private final PaymentGatewayClient gatewayClient;
    private final PaymentRequestFingerprinter fingerprinter;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int maxAttempts;

    public PaymentService(
            PaymentTransactionRepository transactionRepository,
            PaymentEventRepository eventRepository,
            PaymentGatewayClient gatewayClient,
            PaymentRequestFingerprinter fingerprinter,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${payments.gateway.max-attempts:3}") int maxAttempts
    ) {
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.gatewayClient = gatewayClient;
        this.fingerprinter = fingerprinter;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Transactional
    public PaymentServiceResult authorize(PaymentCommand command, String rawIdempotencyKey) {
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        String fingerprint = fingerprinter.fingerprint(command);

        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> replayOrReject(existing, fingerprint))
                .orElseGet(() -> authorizeNewPayment(command, idempotencyKey, fingerprint));
    }

    @Transactional(readOnly = true)
    public PaymentTransaction getPayment(UUID paymentId) {
        return transactionRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private PaymentServiceResult replayOrReject(PaymentTransaction existing, String fingerprint) {
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(
                    "idempotency key was already used for a different payment request"
            );
        }

        return new PaymentServiceResult(existing, true);
    }

    private PaymentServiceResult authorizeNewPayment(
            PaymentCommand command,
            String idempotencyKey,
            String fingerprint
    ) {
        Instant now = clock.instant();
        PaymentTransaction transaction = transactionRepository.save(PaymentTransaction.create(
                idempotencyKey,
                fingerprint,
                command.externalOrderId(),
                command.buyerLicense(),
                command.sellerLicense(),
                command.amount(),
                command.currency(),
                now
        ));
        appendEvent(transaction, PaymentEventType.PAYMENT_CREATED, Map.of(
                "externalOrderId", transaction.getExternalOrderId(),
                "amount", transaction.getAmount(),
                "currency", transaction.getCurrency()
        ));

        PaymentGatewayRequest gatewayRequest = new PaymentGatewayRequest(
                transaction.getId(),
                transaction.getExternalOrderId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                command.paymentMethodToken()
        );

        authorizeWithRetries(transaction, gatewayRequest);
        return new PaymentServiceResult(transaction, false);
    }

    private void authorizeWithRetries(PaymentTransaction transaction, PaymentGatewayRequest gatewayRequest) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            appendEvent(transaction, PaymentEventType.AUTHORIZATION_ATTEMPTED, Map.of("attempt", attempt));

            try {
                GatewayAuthorization authorization = gatewayClient.authorize(gatewayRequest);

                if (authorization.approved()) {
                    transaction.markAuthorized(authorization.providerTransactionId(), clock.instant());
                    appendEvent(transaction, PaymentEventType.PAYMENT_AUTHORIZED, Map.of(
                            "providerTransactionId", authorization.providerTransactionId()
                    ));
                } else {
                    transaction.markDeclined(authorization.message(), clock.instant());
                    appendEvent(transaction, PaymentEventType.PAYMENT_DECLINED, Map.of(
                            "reason", authorization.message()
                    ));
                }

                return;
            } catch (TransientGatewayException ex) {
                if (attempt == maxAttempts) {
                    transaction.markFailed(ex.getMessage(), clock.instant());
                    appendEvent(transaction, PaymentEventType.PAYMENT_FAILED, Map.of(
                            "reason", ex.getMessage(),
                            "attempts", attempt
                    ));
                }
            }
        }
    }

    private void appendEvent(
            PaymentTransaction transaction,
            PaymentEventType eventType,
            Map<String, ?> payloadAttributes
    ) {
        Map<String, ?> payload = Map.of(
                "paymentId", transaction.getId(),
                "status", transaction.getStatus(),
                "attributes", payloadAttributes
        );
        eventRepository.save(PaymentEvent.create(transaction.getId(), eventType, writeJson(payload), clock.instant()));
    }

    private String writeJson(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("could not serialize payment event payload", ex);
        }
    }

    private String normalizeIdempotencyKey(String rawIdempotencyKey) {
        if (rawIdempotencyKey == null || rawIdempotencyKey.isBlank()) {
            throw new InvalidIdempotencyKeyException("Idempotency-Key header is required");
        }

        String idempotencyKey = rawIdempotencyKey.trim();
        if (idempotencyKey.length() > 128) {
            throw new InvalidIdempotencyKeyException("Idempotency-Key must be 128 characters or fewer");
        }

        return idempotencyKey;
    }
}
