package com.bank4z.backend.accountservice;

import com.bank4z.backend.authservice.User;
import com.bank4z.backend.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account extends BaseAuditableEntity {

    public enum Status { ACTIVE, FROZEN, CLOSED }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    protected Account() {
        // required by JPA
    }

    public Account(User user, String accountNumber) {
        this.user = user;
        this.accountNumber = accountNumber;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public Status getStatus() { return status; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(Status status) { this.status = status; }
}