package com.portfolio.payments.gateway;

public interface PaymentGatewayClient {

    GatewayAuthorization authorize(PaymentGatewayRequest request);
}
