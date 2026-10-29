package com.bank4z.backend.accountservice;

import com.bank4z.backend.accountservice.dto.TransferRequest;
import com.bank4z.backend.accountservice.dto.TransferResponse;
import com.bank4z.backend.authservice.User;
import com.bank4z.backend.notificationservice.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final String SENDER_NUMBER = "4Z00000001";
    private static final String RECIPIENT_NUMBER = "4Z00000002";

    @Mock AccountService accountService;
    @Mock AccountRepository accountRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock NotificationService notificationService;
    @InjectMocks TransferService transferService;

    private final UUID senderUserId = UUID.randomUUID();
    private Account sender;
    private Account recipient;

    @BeforeEach
    void setUp() {
        User senderUser = new User("Sender", "sender@test.com", "0000000000001", "0821111111", "hash");
        User recipientUser = new User("Recipient", "recipient@test.com", "0000000000002", "0822222222", "hash");
        sender = new Account(senderUser, SENDER_NUMBER);
        recipient = new Account(recipientUser, RECIPIENT_NUMBER);
    }

    private void stubLookups() {
        when(accountService.getAccountForUser(senderUserId)).thenReturn(sender);
        when(accountRepository.findByAccountNumberForUpdate(SENDER_NUMBER)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumberForUpdate(RECIPIENT_NUMBER)).thenReturn(Optional.of(recipient));
    }

    @Test
    void movesMoneyAndRecordsBothSides() {
        stubLookups();
        sender.setBalance(new BigDecimal("100.00"));

        TransferResponse response = transferService.transfer(
                senderUserId, new TransferRequest(RECIPIENT_NUMBER, new BigDecimal("40.50"), null));

        assertThat(sender.getBalance()).isEqualByComparingTo("59.50");
        assertThat(recipient.getBalance()).isEqualByComparingTo("40.50");
        assertThat(response.newBalance()).isEqualByComparingTo("59.50");
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(notificationService).notifyTransferSent(sender.getUser(), new BigDecimal("40.50"), RECIPIENT_NUMBER);
        verify(notificationService).notifyTransferReceived(recipient.getUser(), new BigDecimal("40.50"), SENDER_NUMBER);
    }

    @Test
    void rejectsWhenFundsAreInsufficient() {
        stubLookups();
        sender.setBalance(new BigDecimal("10.00"));

        assertThatThrownBy(() -> transferService.transfer(
                senderUserId, new TransferRequest(RECIPIENT_NUMBER, new BigDecimal("50.00"), null)))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("10.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("0.00");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void rejectsTransferToSelf() {
        when(accountService.getAccountForUser(senderUserId)).thenReturn(sender);

        assertThatThrownBy(() -> transferService.transfer(
                senderUserId, new TransferRequest(SENDER_NUMBER, new BigDecimal("5.00"), null)))
                .isInstanceOf(InvalidTransferException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void rejectsInactiveRecipient() {
        stubLookups();
        sender.setBalance(new BigDecimal("100.00"));
        recipient.setStatus(Account.Status.FROZEN);

        assertThatThrownBy(() -> transferService.transfer(
                senderUserId, new TransferRequest(RECIPIENT_NUMBER, new BigDecimal("5.00"), null)))
                .isInstanceOf(InvalidTransferException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void rejectsUnknownRecipient() {
        when(accountService.getAccountForUser(senderUserId)).thenReturn(sender);
        when(accountRepository.findByAccountNumberForUpdate(SENDER_NUMBER)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumberForUpdate("4Z00000099")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(
                senderUserId, new TransferRequest("4Z00000099", new BigDecimal("5.00"), null)))
                .isInstanceOf(AccountNotFoundException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}