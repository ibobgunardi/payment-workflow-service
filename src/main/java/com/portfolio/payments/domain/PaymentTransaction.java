package com.portfolio.payments.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "external_order_id", nullable = false, length = 128)
    private String externalOrderId;

    @Column(name = "buyer_license", nullable = false, length = 128)
    private String buyerLicense;

    @Column(name = "seller_license", nullable = false, length = 128)
    private String sellerLicense;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "provider_transaction_id", length = 128)
    private String providerTransactionId;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentTransaction() {
    }

    private PaymentTransaction(
            UUID id,
            String idempotencyKey,
            String requestFingerprint,
            String externalOrderId,
            String buyerLicense,
            String sellerLicense,
            BigDecimal amount,
            String currency,
            Instant now
    ) {
        this.id = Objects.requireNonNull(id);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.requestFingerprint = Objects.requireNonNull(requestFingerprint);
        this.externalOrderId = Objects.requireNonNull(externalOrderId);
        this.buyerLicense = Objects.requireNonNull(buyerLicense);
        this.sellerLicense = Objects.requireNonNull(sellerLicense);
        this.amount = Objects.requireNonNull(amount);
        this.currency = Objects.requireNonNull(currency);
        this.status = PaymentStatus.PENDING;
        this.createdAt = Objects.requireNonNull(now);
        this.updatedAt = now;
    }

    public static PaymentTransaction create(
            String idempotencyKey,
            String requestFingerprint,
            String externalOrderId,
            String buyerLicense,
            String sellerLicense,
            BigDecimal amount,
            String currency,
            Instant now
    ) {
        return new PaymentTransaction(
                UUID.randomUUID(),
                idempotencyKey,
                requestFingerprint,
                externalOrderId,
                buyerLicense,
                sellerLicense,
                amount,
                currency,
                now
        );
    }

    public void markAuthorized(String providerTransactionId, Instant now) {
        this.status = PaymentStatus.AUTHORIZED;
        this.providerTransactionId = Objects.requireNonNull(providerTransactionId);
        this.failureReason = null;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void markDeclined(String reason, Instant now) {
        this.status = PaymentStatus.DECLINED;
        this.failureReason = reason;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void markFailed(String reason, Instant now) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public String getBuyerLicense() {
        return buyerLicense;
    }

    public String getSellerLicense() {
        return sellerLicense;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
