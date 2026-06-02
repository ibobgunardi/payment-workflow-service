package com.portfolio.payments.api;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.payments.api.dto.CreatePaymentRequest;
import com.portfolio.payments.api.dto.PaymentResponse;
import com.portfolio.payments.domain.PaymentTransaction;
import com.portfolio.payments.service.PaymentService;
import com.portfolio.payments.service.PaymentServiceResult;

@Validated
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> authorizePayment(
            @RequestHeader(name = "Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentServiceResult result = paymentService.authorize(request.toCommand(), idempotencyKey);
        PaymentResponse response = PaymentResponse.from(result.transaction(), result.replayed());

        if (result.replayed()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity
                .created(URI.create("/api/payments/" + response.id()))
                .body(response);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        PaymentTransaction transaction = paymentService.getPayment(paymentId);
        return PaymentResponse.from(transaction, false);
    }
}
