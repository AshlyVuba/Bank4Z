package com.bank4z.backend.fraudservice;

import com.bank4z.backend.accountservice.Account;
import com.bank4z.backend.accountservice.Transaction;
import com.bank4z.backend.accountservice.TransactionRepository;
import com.bank4z.backend.fraudservice.dto.FraudFlagResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class FraudService {

    private final TransactionRepository transactionRepository;
    private final FraudFlagRepository fraudFlagRepository;

    private final BigDecimal largeTransferThreshold;
    private final int rapidTransferCount;
    private final int rapidTransferWindowMinutes;

    public FraudService(
            TransactionRepository transactionRepository,
            FraudFlagRepository fraudFlagRepository,
            @Value("${bank4z.fraud.large-transfer-threshold:5000.00}") BigDecimal largeTransferThreshold,
            @Value("${bank4z.fraud.rapid-transfer-count:5}") int rapidTransferCount,
            @Value("${bank4z.fraud.rapid-transfer-window-minutes:10}") int rapidTransferWindowMinutes) {
        this.transactionRepository = transactionRepository;
        this.fraudFlagRepository = fraudFlagRepository;
        this.largeTransferThreshold = largeTransferThreshold;
        this.rapidTransferCount = rapidTransferCount;
        this.rapidTransferWindowMinutes = rapidTransferWindowMinutes;
    }

    /**
     * Runs every rule against a transfer that is about to happen and returns
     * the most severe verdict, or empty if nothing looks off.
     *
     * Called from inside TransferService's transaction, after the sender's
     * account row is locked. That lock matters here: two simultaneous
     * transfers from the same account can't both read "4 recent transfers"
     * and both slip through, because the second one waits for the first.
     *
     * Adding a rule later (e.g. new-device login in B4Z-12) means adding a
     * check here and a value in FraudFlag.Rule. The rest of the flow stays put.
     */
    public Optional<FraudVerdict> evaluate(Account sender, BigDecimal amount) {

        // Rule 1: RAPID_TRANSFERS (HIGH, blocks). Counts only COMPLETED transfers,
        // so blocked attempts don't extend the cool-off. The window slides and
        // clears itself.
        Instant windowStart = Instant.now().minus(Duration.ofMinutes(rapidTransferWindowMinutes));
        long recentTransfers = transactionRepository.countByAccountIdAndTypeAndStatusAndCreatedAtAfter(
                sender.getId(),
                Transaction.Type.TRANSFER_OUT,
                Transaction.Status.COMPLETED,
                windowStart);

        if (recentTransfers >= rapidTransferCount) {
            return Optional.of(new FraudVerdict(
                    FraudFlag.Rule.RAPID_TRANSFERS,
                    FraudFlag.RiskLevel.HIGH,
                    FraudFlag.Action.BLOCKED,
                    recentTransfers + " completed transfers in the last " + rapidTransferWindowMinutes
                            + " minutes (limit " + rapidTransferCount + ")"));
        }

        // Rule 2: LARGE_TRANSFER (MEDIUM, allowed but flagged for admin review)
        if (amount.compareTo(largeTransferThreshold) >= 0) {
            return Optional.of(new FraudVerdict(
                    FraudFlag.Rule.LARGE_TRANSFER,
                    FraudFlag.RiskLevel.MEDIUM,
                    FraudFlag.Action.ALLOWED,
                    "Transfer of R" + amount.toPlainString() + " is at or above the R"
                            + largeTransferThreshold.toPlainString() + " review threshold"));
        }

        return Optional.empty();
    }

    @Transactional
    public FraudFlag recordFlag(Transaction transaction, FraudVerdict verdict) {
        return fraudFlagRepository.save(new FraudFlag(
                transaction,
                verdict.rule(),
                verdict.reason(),
                verdict.riskLevel(),
                verdict.action()));
    }

    // ---- admin review ----

    @Transactional(readOnly = true)
    public Page<FraudFlagResponse> listFlags(Boolean resolved, Pageable pageable) {
        Page<FraudFlag> page = (resolved == null)
                ? fraudFlagRepository.findAll(pageable)
                : fraudFlagRepository.findByResolved(resolved, pageable);
        return page.map(FraudFlagResponse::from);
    }

    /** TODO(B4Z-14): record who resolved which flag in the audit log. */
    @Transactional
    public FraudFlagResponse resolveFlag(UUID id) {
        FraudFlag flag = fraudFlagRepository.findById(id)
                .orElseThrow(() -> new FraudFlagNotFoundException("We couldn't find that fraud flag"));
        flag.markResolved();
        return FraudFlagResponse.from(flag);
    }
}