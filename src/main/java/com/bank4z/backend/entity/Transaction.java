package com.bank4z.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction extends BaseAuditableEntity {

    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT }
    public enum Status { PENDING, COMPLETED, FAILED, FLAGGED }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(length = 100)
    private String reference;

    protected Transaction() {
        // required by JPA
    }

    public Transaction(Account account, BigDecimal amount, Type type, String reference) {
        this.account = account;
        this.amount = amount;
        this.type = type;
        this.reference = reference;
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public BigDecimal getAmount() { return amount; }
    public Type getType() { return type; }
    public Status getStatus() { return status; }
    public String getReference() { return reference; }

    public void setStatus(Status status) { this.status = status; }
}