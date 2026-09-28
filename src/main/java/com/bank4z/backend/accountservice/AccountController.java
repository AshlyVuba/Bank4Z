package com.bank4z.backend.accountservice;

import com.bank4z.backend.accountservice.dto.AccountResponse;
import com.bank4z.backend.accountservice.dto.TransactionResponse;
import com.bank4z.backend.authservice.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public AccountController(AccountService accountService, TransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/me")
    public AccountResponse getMyAccount(@AuthenticationPrincipal UserPrincipal principal) {
        Account account = accountService.getAccountForUser(principal.getId());
        return AccountResponse.from(account);
    }

    @GetMapping("/me/transactions")
    public Page<TransactionResponse> getMyTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Account account = accountService.getAccountForUser(principal.getId());
        // Capped server-side so a client can't request an unreasonably large page
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));

        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(account.getId(), pageable)
                .map(TransactionResponse::from);
    }
}