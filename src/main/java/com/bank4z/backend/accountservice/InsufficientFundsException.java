package com.bank4z.backend.accountservice;

public class InsufficientFundsException extends RuntimeException {

    public static final String CODE = "INSUFFICIENT_FUNDS";

    public InsufficientFundsException() {
        super("Jaden broke and so are you 😭");
    }
}