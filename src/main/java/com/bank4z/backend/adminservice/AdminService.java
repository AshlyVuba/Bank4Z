package com.bank4z.backend.adminservice;

import com.bank4z.backend.accountservice.Account;
import com.bank4z.backend.accountservice.AccountNotFoundException;
import com.bank4z.backend.accountservice.AccountRepository;
import com.bank4z.backend.authservice.User;
import com.bank4z.backend.authservice.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public AdminService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Uses the same row lock as transfers, so a freeze can't slip in halfway
     * through a payment touching that account.
     * TODO(B4Z-14): record this action in the audit log (who froze what, when).
     */
    @Transactional
    public Account updateAccountStatus(String accountNumber, Account.Status status) {
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("We couldn't find that account number 🤷"));
        account.setStatus(status);
        return account;
    }
}