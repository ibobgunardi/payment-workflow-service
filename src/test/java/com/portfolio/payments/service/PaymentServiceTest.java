package com.portfolio.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import com.portfolio.payments.domain.PaymentEventType;
import com.portfolio.payments.domain.PaymentStatus;
import com.portfolio.payments.gateway.GatewayAuthorization;
import com.portfolio.payments.gateway.PaymentGatewayClient;
import com.portfolio.payments.gateway.PaymentGatewayRequest;
import com.portfolio.payments.gateway.TransientGatewayException;
import com.portfolio.payments.repository.PaymentEventRepository;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payments;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "payments.outbox.publish-delay-ms=600000"
})
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentEventRepository eventRepository;

    @Autowired
    private TestGatewayClient gatewayClient;

    @BeforeEach
    void resetGateway() {
        gatewayClient.reset();
    }

    @Test
    void replaysSameIdempotencyKeyWithoutCallingGatewayTwice() {
        PaymentCommand command = command("order-1001", "tok_success");

        PaymentServiceResult first = paymentService.authorize(command, "idem-order-1001");
        PaymentServiceResult second = paymentService.authorize(command, "idem-order-1001");

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.transaction().getId()).isEqualTo(first.transaction().getId());
        assertThat(second.transaction().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(gatewayClient.calls()).isEqualTo(1);
    }

    @Test
    void rejectsSameIdempotencyKeyWithDifferentPayload() {
        paymentService.authorize(command("order-1002", "tok_success"), "idem-order-1002");

        PaymentCommand changedCommand = new PaymentCommand(
                "order-1002",
                "CA-DISP-25",
                "CA-BRAND-44",
                new BigDecimal("251.00"),
                "USD",
                "tok_success"
        );

        assertThatThrownBy(() -> paymentService.authorize(changedCommand, "idem-order-1002"))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("different payment request");
        assertThat(gatewayClient.calls()).isEqualTo(1);
    }

    @Test
    void retriesTransientGatewayFailuresBeforeAuthorizing() {
        gatewayClient.failuresBeforeApproval(2);

        PaymentServiceResult result = paymentService.authorize(command("order-1003", "tok_flaky"), "idem-order-1003");

        assertThat(result.transaction().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(result.transaction().getProviderTransactionId()).isEqualTo("test_provider_tx_3");
        assertThat(gatewayClient.calls()).isEqualTo(3);

        List<PaymentEventType> eventTypes = eventRepository
                .findByTransactionIdOrderByCreatedAtAsc(result.transaction().getId())
                .stream()
                .map(event -> event.getEventType())
                .toList();

        assertThat(eventTypes).containsExactly(
                PaymentEventType.PAYMENT_CREATED,
                PaymentEventType.AUTHORIZATION_ATTEMPTED,
                PaymentEventType.AUTHORIZATION_ATTEMPTED,
                PaymentEventType.AUTHORIZATION_ATTEMPTED,
                PaymentEventType.PAYMENT_AUTHORIZED
        );
    }

    private PaymentCommand command(String externalOrderId, String token) {
        return new PaymentCommand(
                externalOrderId,
                "CA-DISP-25",
                "CA-BRAND-44",
                new BigDecimal("250.00"),
                "usd",
                token
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        Clock tickingClock() {
            return new TickingClock(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        @Primary
        TestGatewayClient testGatewayClient() {
            return new TestGatewayClient();
        }
    }

    static class TestGatewayClient implements PaymentGatewayClient {

        private final AtomicInteger calls = new AtomicInteger();
        private int failuresBeforeApproval;

        @Override
        public GatewayAuthorization authorize(PaymentGatewayRequest request) {
            int currentCall = calls.incrementAndGet();
            if (failuresBeforeApproval > 0) {
                failuresBeforeApproval--;
                throw new TransientGatewayException("temporary payment provider outage");
            }

            return GatewayAuthorization.approved("test_provider_tx_" + currentCall);
        }

        void failuresBeforeApproval(int failuresBeforeApproval) {
            this.failuresBeforeApproval = failuresBeforeApproval;
        }

        int calls() {
            return calls.get();
        }

        void reset() {
            calls.set(0);
            failuresBeforeApproval = 0;
        }
    }

    static class TickingClock extends Clock {

        private final Instant baseInstant;
        private final ZoneId zone;
        private final AtomicLong ticks = new AtomicLong();

        TickingClock(Instant baseInstant, ZoneId zone) {
            this.baseInstant = baseInstant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new TickingClock(baseInstant, zone);
        }

        @Override
        public Instant instant() {
            return baseInstant.plusMillis(ticks.getAndIncrement());
        }
    }
}
