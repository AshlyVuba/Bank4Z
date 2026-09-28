package com.bank4z.backend.accountservice;

import com.bank4z.backend.authservice.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountService(AccountRepository accountRepository, AccountNumberGenerator accountNumberGenerator) {
        this.accountRepository = accountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @Transactional
    public Account createAccountForUser(User user) {
        String accountNumber = accountNumberGenerator.generate();
        Account account = new Account(user, accountNumber);
        return accountRepository.save(account);
    }

    public Account getAccountForUser(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("No account found for this user"));
    }
}