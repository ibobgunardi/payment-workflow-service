package com.portfolio.payments.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.payments.domain.PaymentEvent;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    List<PaymentEvent> findByPublishedAtIsNullOrderByCreatedAtAsc(Pageable pageable);

    List<PaymentEvent> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);
}
