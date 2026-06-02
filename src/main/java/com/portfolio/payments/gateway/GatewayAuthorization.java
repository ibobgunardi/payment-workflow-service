package com.portfolio.payments.gateway;

public record GatewayAuthorization(
        boolean approved,
        String providerTransactionId,
        String message
) {

    public static GatewayAuthorization approved(String providerTransactionId) {
        return new GatewayAuthorization(true, providerTransactionId, "approved");
    }

    public static GatewayAuthorization declined(String message) {
        return new GatewayAuthorization(false, null, message);
    }
}
