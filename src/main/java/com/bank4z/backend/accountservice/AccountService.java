package com.bank4z.backend.accountservice;

import com.bank4z.backend.authservice.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountService(AccountRepository accountRepository, AccountNumberGenerator accountNumberGenerator) {
        this.accountRepository = accountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    /**
     * Called right after a user registers. Balance starts at 0 by the
     * Account entity's default — nothing to set explicitly here.
     */
    @Transactional
    public Account createAccountForUser(User user) {
        String accountNumber = accountNumberGenerator.generate();
        Account account = new Account(user, accountNumber);
        return accountRepository.save(account);
    }
}