package com.portfolio.payments.gateway;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public GatewayAuthorization authorize(PaymentGatewayRequest request) {
        String token = request.paymentMethodToken().toLowerCase(Locale.ROOT);

        if (token.startsWith("timeout")) {
            throw new TransientGatewayException("payment provider timed out");
        }

        if (token.startsWith("decline")) {
            return GatewayAuthorization.declined("payment provider declined authorization");
        }

        return GatewayAuthorization.approved("anet_" + UUID.randomUUID());
    }
}
