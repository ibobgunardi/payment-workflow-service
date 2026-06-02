package com.portfolio.payments.outbox;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.payments.domain.PaymentEvent;
import com.portfolio.payments.repository.PaymentEventRepository;

@Component
public class PaymentOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxPublisher.class);

    private final PaymentEventRepository eventRepository;
    private final Clock clock;
    private final int batchSize;

    public PaymentOutboxPublisher(
            PaymentEventRepository eventRepository,
            Clock clock,
            @Value("${payments.outbox.batch-size:50}") int batchSize
    ) {
        this.eventRepository = eventRepository;
        this.clock = clock;
        this.batchSize = Math.max(1, batchSize);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${payments.outbox.publish-delay-ms:10000}")
    public void publishPendingEvents() {
        List<PaymentEvent> events = eventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(
                PageRequest.of(0, batchSize)
        );

        for (PaymentEvent event : events) {
            log.info(
                    "publishing payment event id={} transactionId={} type={} payload={}",
                    event.getId(),
                    event.getTransactionId(),
                    event.getEventType(),
                    event.getPayload()
            );
            event.markPublished(clock.instant());
        }
    }
}
