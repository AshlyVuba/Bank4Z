package com.bank4z.backend.accountservice;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final String PREFIX = "4Z"; // Bank4Z accounts are visually recognizable
    private static final int RANDOM_DIGITS = 8;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accountRepository;

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Generates an account number like "4Z83920174" and guarantees it doesn't
     * already exist in the database. Uses SecureRandom rather than
     * Math.random() — account numbers are sensitive enough to warrant a
     * cryptographically strong source, not just "good enough" randomness.
     */
    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = PREFIX + randomDigits(RANDOM_DIGITS);
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique account number after " + MAX_ATTEMPTS + " attempts");
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}