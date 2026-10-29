package com.bank4z.backend.accountservice;

import com.bank4z.backend.accountservice.dto.TransferRequest;
import com.bank4z.backend.accountservice.dto.TransferResponse;
import com.bank4z.backend.notificationservice.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public TransferService(AccountService accountService,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           NotificationService notificationService) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    /**
     * Everything below runs in ONE database transaction: either both balances
     * change and both transaction rows are written, or nothing happens at all.
     *
     * Fraud checks (B4Z-11) will plug in right before the money moves.
     */
    @Transactional
    public TransferResponse transfer(UUID senderUserId, TransferRequest request) {
        // The sender comes from the authenticated user, never from the request body.
        Account senderOwn = accountService.getAccountForUser(senderUserId);
        String senderNumber = senderOwn.getAccountNumber();
        String recipientNumber = request.recipientAccountNumber().trim();

        if (senderNumber.equals(recipientNumber)) {
            throw new InvalidTransferException("You can't send money to yourself, bestie");
        }

        // Always lock in the same order (lowest account number first). If A->B and
        // B->A run at the same time, both lock A first, so they queue up instead
        // of each holding one lock and waiting forever on the other (deadlock).
        boolean senderFirst = senderNumber.compareTo(recipientNumber) < 0;
        Account first = lockAccount(senderFirst ? senderNumber : recipientNumber);
        Account second = lockAccount(senderFirst ? recipientNumber : senderNumber);
        Account sender = senderFirst ? first : second;
        Account recipient = senderFirst ? second : first;

        if (sender.getStatus() != Account.Status.ACTIVE) {
            throw new InvalidTransferException("Your account isn't active, so you can't send money right now");
        }
        if (recipient.getStatus() != Account.Status.ACTIVE) {
            throw new InvalidTransferException("That account can't receive money right now");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        // Both accounts are managed entities inside this transaction, so these
        // changes are flushed automatically on commit.
        sender.setBalance(sender.getBalance().subtract(amount));
        recipient.setBalance(recipient.getBalance().add(amount));

        boolean hasNote = request.reference() != null && !request.reference().isBlank();
        String senderRef = hasNote ? request.reference().trim() : "Transfer to " + recipientNumber;
        String recipientRef = hasNote ? request.reference().trim() : "Transfer from " + senderNumber;

        Transaction debit = new Transaction(sender, amount, Transaction.Type.TRANSFER_OUT, senderRef);
        debit.setStatus(Transaction.Status.COMPLETED);
        Transaction credit = new Transaction(recipient, amount, Transaction.Type.TRANSFER_IN, recipientRef);
        credit.setStatus(Transaction.Status.COMPLETED);

        transactionRepository.save(debit);
        transactionRepository.save(credit);

        // Runs inside the same database transaction as the transfer for now.
        // In Iteration 4 (B4Z-15/17) this becomes a published event consumed by
        // the notification service, so a notification problem can never block
        // or undo a payment.
        notificationService.notifyTransferSent(sender.getUser(), amount, recipientNumber);
        notificationService.notifyTransferReceived(recipient.getUser(), amount, senderNumber);

        return new TransferResponse(
                debit.getId(),
                recipientNumber,
                amount,
                sender.getBalance(),
                "Sent! Money moved, no cap 💸"
        );
    }

    private Account lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("We couldn't find that account number 🤷"));
    }
}