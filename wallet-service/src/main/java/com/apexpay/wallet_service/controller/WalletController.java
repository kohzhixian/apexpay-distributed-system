package com.apexpay.wallet_service.controller;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.wallet_service.dto.*;
import com.apexpay.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for wallet operations.
 * Handles wallet creation, top-up, transfers, and payments.
 */
@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /** Creates a new wallet for the authenticated user. */
    @PostMapping
    public ResponseEntity<CreateWalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        CreateWalletResponse response = walletService.createWallet(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Adds funds to an existing wallet. */
    @PostMapping("/topup")
    public ResponseEntity<TopUpWalletResponse> topUpWallet(@Valid @RequestBody TopUpWalletRequest request,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        TopUpWalletResponse response = walletService.topUpWallet(request, userId);
        return ResponseEntity.ok(response);
    }

    /** Transfers funds between two wallets. */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
            @RequestHeader(HttpHeaders.X_USER_ID) String payerUserId) {
        TransferResponse response = walletService.transfer(request, payerUserId);
        return ResponseEntity.ok(response);
    }

    /** Processes a payment from a wallet. */
    @PostMapping("/payment")
    public ResponseEntity<PaymentResponse> payment(@Valid @RequestBody PaymentRequest request,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        PaymentResponse response = walletService.payment(request, userId);
        return ResponseEntity.ok(response);
    }
}
