package com.portfolio.payments.gateway;

public class TransientGatewayException extends RuntimeException {

    public TransientGatewayException(String message) {
        super(message);
    }
}
