package com.bank4z.backend.accountservice;

public class InvalidTransferException extends RuntimeException {

    public static final String CODE = "INVALID_TRANSFER";

    public InvalidTransferException(String message) {
        super(message);
    }
}