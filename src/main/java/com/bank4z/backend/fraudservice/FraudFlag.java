package com.bank4z.backend.fraudservice;

import com.bank4z.backend.accountservice.Transaction;
import com.bank4z.backend.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "fraud_flags")
public class FraudFlag extends BaseAuditableEntity {

    public enum Rule { LARGE_TRANSFER, RAPID_TRANSFERS }
    public enum RiskLevel { MEDIUM, HIGH }
    public enum Action { ALLOWED, BLOCKED }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_code", nullable = false, length = 30)
    private Rule rule;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", nullable = false, length = 10)
    private Action actionTaken;

    @Column(nullable = false)
    private boolean resolved = false;

    protected FraudFlag() {
        // required by JPA
    }

    public FraudFlag(Transaction transaction, Rule rule, String reason, RiskLevel riskLevel, Action actionTaken) {
        this.transaction = transaction;
        this.rule = rule;
        this.reason = reason;
        this.riskLevel = riskLevel;
        this.actionTaken = actionTaken;
    }

    public UUID getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public Rule getRule() { return rule; }
    public String getReason() { return reason; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public Action getActionTaken() { return actionTaken; }
    public boolean isResolved() { return resolved; }

    public void markResolved() { this.resolved = true; }
}