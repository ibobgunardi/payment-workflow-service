create table payment_transactions (
    id uuid primary key,
    idempotency_key varchar(128) not null,
    request_fingerprint varchar(64) not null,
    external_order_id varchar(128) not null,
    buyer_license varchar(128) not null,
    seller_license varchar(128) not null,
    amount numeric(19, 4) not null,
    currency varchar(3) not null,
    status varchar(32) not null,
    provider_transaction_id varchar(128),
    failure_reason varchar(512),
    version bigint not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_payment_transactions_idempotency_key unique (idempotency_key)
);

create index idx_payment_transactions_external_order_id
    on payment_transactions (external_order_id);

create index idx_payment_transactions_status_created_at
    on payment_transactions (status, created_at);

create table payment_events (
    id uuid primary key,
    transaction_id uuid not null references payment_transactions (id),
    event_type varchar(64) not null,
    payload text not null,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone,
    constraint ck_payment_events_payload_not_blank check (length(payload) > 0)
);

create index idx_payment_events_published_at_created_at
    on payment_events (published_at, created_at);

create index idx_payment_events_transaction_id
    on payment_events (transaction_id);
