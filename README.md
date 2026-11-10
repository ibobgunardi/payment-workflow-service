# Payment Workflow Service

[![CI](https://github.com/ibobgunardi/payment-workflow-service/actions/workflows/ci.yml/badge.svg)](https://github.com/ibobgunardi/payment-workflow-service/actions/workflows/ci.yml)

Small Java/Spring Boot portfolio service modeled around financial workflow backend work: idempotent payment authorization, relational persistence, retry handling, and outbox-style event publication.

## Overview

A small Spring Boot service for payment authorization: idempotent requests, retries for gateway timeouts, Postgres with schema migrations, and outbox-style event publishing. It's kept compact so the design is easy to read.

## Architecture

- `POST /api/payments` creates or replays a payment authorization by `Idempotency-Key`.
- Duplicate idempotency key with the same request returns the stored transaction.
- Duplicate idempotency key with a changed request returns `409 Conflict`.
- A simulated Authorize.Net-style gateway approves, declines, or times out based on the token prefix.
- Transient gateway failures are retried before the transaction is marked `FAILED`.
- Domain events are written to `payment_events` in the same database transaction.
- A scheduled outbox publisher logs unpublished events and marks them published.

## Run

With Docker:

```bash
docker compose up --build
```

Without Docker, start Postgres locally and run:

```bash
mvn spring-boot:run
```

The app listens on `http://localhost:8080`.

## Try it

Authorize a payment:

```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-1001-v1" \
  -d '{
    "externalOrderId": "order-1001",
    "buyerLicense": "CA-DISP-25",
    "sellerLicense": "CA-BRAND-44",
    "amount": 250.00,
    "currency": "USD",
    "paymentMethodToken": "tok_success"
  }'
```

Replay the exact same request with the same idempotency key. The response returns `200 OK` with `"replayed": true`.

Simulate a decline:

```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-1002-v1" \
  -d '{
    "externalOrderId": "order-1002",
    "buyerLicense": "CA-DISP-25",
    "sellerLicense": "CA-BRAND-44",
    "amount": 125.00,
    "currency": "USD",
    "paymentMethodToken": "decline_card"
  }'
```

Simulate repeated provider timeouts:

```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-1003-v1" \
  -d '{
    "externalOrderId": "order-1003",
    "buyerLicense": "CA-DISP-25",
    "sellerLicense": "CA-BRAND-44",
    "amount": 99.00,
    "currency": "USD",
    "paymentMethodToken": "timeout_provider"
  }'
```

## Test

```bash
mvn test
```

The tests cover:

- Idempotent replay without a second gateway call
- Conflict detection when an idempotency key is reused with changed payload
- Retry behavior for transient gateway failures
- Outbox event sequence for a retried authorization

## Design Notes

Useful points to discuss:

- Why idempotency keys are mandatory for payment APIs
- Why the database unique constraint is still needed even with application-level checks
- Why events are stored in an outbox table instead of published directly inside the request path
- How this would evolve to Kafka/SQS, provider webhooks, reconciliation jobs, fraud checks, and ledger entries
- What would change if the gateway call had to happen outside the database transaction

## Tradeoffs

This demo keeps the request path compact so it is easy to read and discuss. In a production payment service, the next improvements would be:

- Reserve the idempotency key in a short transaction, call the gateway outside the database transaction, then finalize with optimistic locking.
- Convert the simulated gateway to a provider adapter with signed requests, provider request IDs, and webhook reconciliation.
- Publish outbox rows to Kafka/SQS with retry metadata and dead-letter handling instead of logging them.
- Add a ledger table so authorization, capture, refund, and settlement events can be audited independently.
