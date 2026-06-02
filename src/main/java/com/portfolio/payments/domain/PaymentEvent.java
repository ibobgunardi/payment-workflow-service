package com.portfolio.payments.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_events")
public class PaymentEvent {

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private PaymentEventType eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected PaymentEvent() {
    }

    private PaymentEvent(UUID transactionId, PaymentEventType eventType, String payload, Instant now) {
        this.id = UUID.randomUUID();
        this.transactionId = Objects.requireNonNull(transactionId);
        this.eventType = Objects.requireNonNull(eventType);
        this.payload = Objects.requireNonNull(payload);
        this.createdAt = Objects.requireNonNull(now);
    }

    public static PaymentEvent create(UUID transactionId, PaymentEventType eventType, String payload, Instant now) {
        return new PaymentEvent(transactionId, eventType, payload, now);
    }

    public void markPublished(Instant now) {
        this.publishedAt = Objects.requireNonNull(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public PaymentEventType getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
