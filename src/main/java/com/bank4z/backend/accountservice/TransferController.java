package com.bank4z.backend.accountservice;

import com.bank4z.backend.accountservice.dto.TransferRequest;
import com.bank4z.backend.accountservice.dto.TransferResponse;
import com.bank4z.backend.authservice.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransferRequest request) {

        TransferResponse response = transferService.transfer(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}