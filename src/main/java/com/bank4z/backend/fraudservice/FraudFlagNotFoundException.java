package com.bank4z.backend.fraudservice;

public class FraudFlagNotFoundException extends RuntimeException {
    public FraudFlagNotFoundException(String message) {
        super(message);
    }
}