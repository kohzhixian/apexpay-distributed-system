package com.apexpay.wallet_service.controller;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.common.dto.*;
import com.apexpay.wallet_service.dto.request.CreateWalletRequest;
import com.apexpay.wallet_service.dto.request.TopUpWalletRequest;
import com.apexpay.wallet_service.dto.request.TransferRequest;
import com.apexpay.wallet_service.dto.response.*;
import com.apexpay.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for wallet operations.
 * Handles wallet creation, top-up, transfers, and payments.
 */
@RestController
@RequestMapping("/api/v1/wallet")
@Validated // needed for validation on path variables and request params
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Creates a new wallet for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<CreateWalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request,
                                                             @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        CreateWalletResponse response = walletService.createWallet(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Adds funds to an existing wallet.
     */
    @PostMapping("/topup")
    public ResponseEntity<TopUpWalletResponse> topUpWallet(@Valid @RequestBody TopUpWalletRequest request,
                                                           @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        TopUpWalletResponse response = walletService.topUpWallet(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Transfers funds between two wallets.
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                     @RequestHeader(HttpHeaders.X_USER_ID) String payerUserId) {
        TransferResponse response = walletService.transfer(request, payerUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the current wallet balance for the authenticated user.
     */
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<GetBalanceResponse> getBalance(@PathVariable("walletId") UUID walletId,
                                                         @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        GetBalanceResponse response = walletService.getBalance(walletId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a paged transaction history (10 items per page).
     * The offset is 1-based to align with user-facing pagination.
     */
    @GetMapping("/{walletId}/history/{offset}")
    public ResponseEntity<List<GetTransactionHistoryResponse>> getTransactionHistory(
            @PathVariable("walletId") UUID walletId,
            @PathVariable("offset") int offset,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        List<GetTransactionHistoryResponse> response = walletService.getTransactionHistory(walletId, userId, offset);
        return ResponseEntity.ok(response);
    }

    /**
     * Reserves funds in a wallet for a pending payment.
     * <p>
     * Part of the two-phase commit pattern for payment processing. Moves funds
     * from available balance to reserved balance.
     * </p>
     */
    @PostMapping("/{walletId}/reserve")
    public ResponseEntity<ReserveFundsResponse> reserveFunds(@RequestHeader(HttpHeaders.X_USER_ID) String userId,
                                                             @PathVariable("walletId") UUID walletId,
                                                             @Valid @RequestBody ReserveFundsRequest request) {
        ReserveFundsResponse response = walletService.reserveFunds(request, userId, walletId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Confirms a fund reservation after successful payment.
     * <p>
     * Completes the two-phase commit by finalizing the reservation and
     * deducting funds from the wallet balance.
     * </p>
     */
    @PostMapping("/{walletId}/confirm")
    public ResponseEntity<String> confirmReservation(@Valid @RequestBody ConfirmReservationRequest request,
                                                     @PathVariable("walletId") UUID walletId,
                                                     @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        String response = walletService.confirmReservation(request, userId, walletId);
        return ResponseEntity.ok(response);

    }

    /**
     * Cancels a fund reservation when payment fails.
     * <p>
     * Rolls back the two-phase commit by releasing reserved funds back
     * to available balance.
     * </p>
     */
    @PostMapping("/{walletId}/cancel")
    public ResponseEntity<String> cancelReservation(@Valid @RequestBody CancelReservationRequest request,
                                                    @PathVariable("walletId") UUID walletId,
                                                    @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        String response = walletService.cancelReservation(request, userId, walletId);
        return ResponseEntity.ok(response);
    }
}
