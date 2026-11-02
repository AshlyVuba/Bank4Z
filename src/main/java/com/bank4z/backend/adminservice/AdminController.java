package com.bank4z.backend.adminservice;

import com.bank4z.backend.accountservice.Account;
import com.bank4z.backend.accountservice.dto.AccountResponse;
import com.bank4z.backend.adminservice.dto.AdminUserResponse;
import com.bank4z.backend.adminservice.dto.UpdateAccountStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Two independent layers protect everything here:
 *  1. SecurityConfig: /api/admin/** requires ROLE_ADMIN (URL rule)
 *  2. @PreAuthorize below: the controller itself requires ROLE_ADMIN
 * If one is ever misconfigured, the other still holds.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return adminService.listUsers(pageable).map(AdminUserResponse::from);
    }

    @PutMapping("/accounts/{accountNumber}/status")
    public AccountResponse updateAccountStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateAccountStatusRequest request) {

        Account.Status status = Account.Status.valueOf(request.status().name());
        return AccountResponse.from(adminService.updateAccountStatus(accountNumber, status));
    }
}