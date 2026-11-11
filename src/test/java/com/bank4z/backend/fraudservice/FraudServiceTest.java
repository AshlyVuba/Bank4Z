package com.bank4z.backend.fraudservice;

import com.bank4z.backend.accountservice.Account;
import com.bank4z.backend.accountservice.Transaction;
import com.bank4z.backend.accountservice.TransactionRepository;
import com.bank4z.backend.authservice.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock FraudFlagRepository fraudFlagRepository;

    private FraudService fraudService;
    private Account sender;

    @BeforeEach
    void setUp() {
        // threshold R5000.00, max 5 completed transfers per 10 minutes
        fraudService = new FraudService(
                transactionRepository, fraudFlagRepository, new BigDecimal("5000.00"), 5, 10);

        User user = new User("Sender", "sender@test.com", "0000000000001", "0821111111", "hash");
        sender = new Account(user, "4Z00000001");
    }

    private void recentCompletedTransfers(long count) {
        when(transactionRepository.countByAccountIdAndTypeAndStatusAndCreatedAtAfter(
                any(), eq(Transaction.Type.TRANSFER_OUT), eq(Transaction.Status.COMPLETED), any(Instant.class)))
                .thenReturn(count);
    }

    @Test
    void ordinaryTransferIsNotFlagged() {
        recentCompletedTransfers(0);

        Optional<FraudVerdict> verdict = fraudService.evaluate(sender, new BigDecimal("100.00"));

        assertThat(verdict).isEmpty();
    }

    @Test
    void transferJustBelowThresholdIsNotFlagged() {
        recentCompletedTransfers(0);

        Optional<FraudVerdict> verdict = fraudService.evaluate(sender, new BigDecimal("4999.99"));

        assertThat(verdict).isEmpty();
    }

    @Test
    void transferAtThresholdIsAllowedButFlaggedForReview() {
        recentCompletedTransfers(0);

        FraudVerdict verdict = fraudService.evaluate(sender, new BigDecimal("5000.00")).orElseThrow();

        assertThat(verdict.rule()).isEqualTo(FraudFlag.Rule.LARGE_TRANSFER);
        assertThat(verdict.riskLevel()).isEqualTo(FraudFlag.RiskLevel.MEDIUM);
        assertThat(verdict.action()).isEqualTo(FraudFlag.Action.ALLOWED);
    }

    @Test
    void tooManyRecentTransfersAreBlocked() {
        recentCompletedTransfers(5);

        FraudVerdict verdict = fraudService.evaluate(sender, new BigDecimal("10.00")).orElseThrow();

        assertThat(verdict.rule()).isEqualTo(FraudFlag.Rule.RAPID_TRANSFERS);
        assertThat(verdict.riskLevel()).isEqualTo(FraudFlag.RiskLevel.HIGH);
        assertThat(verdict.action()).isEqualTo(FraudFlag.Action.BLOCKED);
    }

    @Test
    void justUnderTheRapidLimitIsStillAllowed() {
        recentCompletedTransfers(4);

        Optional<FraudVerdict> verdict = fraudService.evaluate(sender, new BigDecimal("10.00"));

        assertThat(verdict).isEmpty();
    }

    @Test
    void rapidRuleOutranksLargeRule() {
        recentCompletedTransfers(5);

        FraudVerdict verdict = fraudService.evaluate(sender, new BigDecimal("9000.00")).orElseThrow();

        assertThat(verdict.action()).isEqualTo(FraudFlag.Action.BLOCKED);
        assertThat(verdict.rule()).isEqualTo(FraudFlag.Rule.RAPID_TRANSFERS);
    }
}