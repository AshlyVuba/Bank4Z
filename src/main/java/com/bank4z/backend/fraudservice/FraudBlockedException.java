package com.bank4z.backend.fraudservice;

/**
 * The customer-facing message is intentionally vague. Telling someone
 * "you made 5 transfers in 10 minutes" would hand an attacker the exact
 * threshold to stay just under. Details go to the flag, not the response.
 */
public class FraudBlockedException extends RuntimeException {

    public static final String CODE = "FRAUD_BLOCKED";

    public FraudBlockedException() {
        super("Hold up, this looks sus 👀 We paused that transfer to keep your money safe. "
                + "Give it a few minutes and try again.");
    }
}