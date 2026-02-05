package com.apexpay.wallet_service.controller;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.common.dto.CancelReservationRequest;
import com.apexpay.common.dto.ConfirmReservationRequest;
import com.apexpay.common.dto.ReserveFundsRequest;
import com.apexpay.common.dto.ReserveFundsResponse;
import com.apexpay.wallet_service.dto.request.CreateWalletRequest;
import com.apexpay.wallet_service.dto.request.TopUpWalletRequest;
import com.apexpay.wallet_service.dto.request.TransferRequest;
import com.apexpay.wallet_service.dto.request.UpdateWalletNameRequest;
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

    /**
     * Constructs a new WalletController with the required service.
     *
     * @param walletService the wallet service for business logic
     */
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Creates a new wallet for the authenticated user.
     *
     * @param request the wallet creation request with name and currency
     * @param userId  the authenticated user's ID from the X-USER-ID header
     * @return 201 Created with wallet details
     */
    @PostMapping
    public ResponseEntity<CreateWalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request,
                                                             @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        CreateWalletResponse response = walletService.createWallet(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Adds funds to an existing wallet.
     *
     * @param request the top-up request with wallet ID and amount
     * @param userId  the authenticated user's ID from the X-USER-ID header
     * @return 200 OK with updated balance information
     */
    @PostMapping("/topup")
    public ResponseEntity<TopUpWalletResponse> topUpWallet(@Valid @RequestBody TopUpWalletRequest request,
                                                           @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        TopUpWalletResponse response = walletService.topUpWallet(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Transfers funds between two wallets.
     *
     * @param request     the transfer request with recipient and amount details
     * @param payerUserId the authenticated user's ID from the X-USER-ID header
     * @return 200 OK with transfer confirmation details
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                     @RequestHeader(HttpHeaders.X_USER_ID) String payerUserId) {
        TransferResponse response = walletService.transfer(request, payerUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the current wallet balance for the authenticated user.
     *
     * @param walletId the ID of the wallet to check
     * @param userId   the authenticated user's ID from the X-USER-ID header
     * @return 200 OK with current balance and currency
     */
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<GetBalanceResponse> getBalance(@PathVariable("walletId") UUID walletId,
                                                         @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        GetBalanceResponse response = walletService.getBalance(walletId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a paged transaction history (10 items per page).
     * <p>
     * If walletId is provided, returns transactions for that specific wallet.
     * If walletId is omitted, returns transactions for all user's wallets.
     * The offset is 1-based to align with user-facing pagination.
     * </p>
     *
     * @param walletId optional wallet ID to filter transactions
     * @param offset   page offset (1-based, defaults to 1)
     * @param userId   the authenticated user's ID from gateway
     * @return list of transactions (max 10 items per page)
     */
    @GetMapping("/history")
    public ResponseEntity<List<GetTransactionHistoryResponse>> getTransactionHistory(
            @RequestParam(required = false) UUID walletId,
            @RequestParam(defaultValue = "1") int offset,
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
     *
     * @param userId   the authenticated user's ID from the X-USER-ID header
     * @param walletId the ID of the wallet to reserve funds from
     * @param request  the reservation request with amount and payment details
     * @return 201 Created with reservation transaction ID and remaining balance
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
     *
     * @param request  the confirmation request with wallet transaction ID
     * @param walletId the ID of the wallet with reserved funds
     * @param userId   the authenticated user's ID from the X-USER-ID header
     * @return 200 OK with confirmation message
     */
    @PostMapping("/{walletId}/confirm")
    public ResponseEntity<ConfirmReservationResponse> confirmReservation(@Valid @RequestBody ConfirmReservationRequest request,
                                                                         @PathVariable("walletId") UUID walletId,
                                                                         @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        ConfirmReservationResponse response = walletService.confirmReservation(request, userId, walletId);
        return ResponseEntity.ok(response);

    }

    /**
     * Cancels a fund reservation when payment fails.
     * <p>
     * Rolls back the two-phase commit by releasing reserved funds back
     * to available balance.
     * </p>
     *
     * @param request  the cancellation request with wallet transaction ID
     * @param walletId the ID of the wallet with reserved funds
     * @param userId   the authenticated user's ID from the X-USER-ID header
     * @return 200 OK with cancellation confirmation message
     */
    @PostMapping("/{walletId}/cancel")
    public ResponseEntity<CancelReservationResponse> cancelReservation(@Valid @RequestBody CancelReservationRequest request,
                                                                       @PathVariable("walletId") UUID walletId,
                                                                       @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        CancelReservationResponse response = walletService.cancelReservation(request, userId, walletId);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the name of an existing wallet.
     *
     * @param walletId the wallet ID to update
     * @param request  the request containing the new wallet name
     * @param userId   the authenticated user's ID from gateway
     * @return 200 OK with success message
     */
    @PatchMapping("/{walletId}/name")
    public ResponseEntity<UpdateWalletNameResponse> updateWalletName(
            @PathVariable("walletId") UUID walletId,
            @Valid @RequestBody UpdateWalletNameRequest request,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        UpdateWalletNameResponse response = walletService.updateWalletName(request, walletId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all wallets for the authenticated user.
     *
     * @param userId the authenticated user's ID from gateway
     * @return 200 OK with list of wallets including IDs, names, balances, and currencies
     */
    @GetMapping("/user")
    public ResponseEntity<List<GetWalletByUserIdResponse>> getWalletByUserId(@RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        List<GetWalletByUserIdResponse> response = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the 5 most recent transactions across all user's wallets.
     *
     * @param userId the authenticated user's ID from gateway
     * @return 200 OK with list of recent transactions (max 5, newest first)
     */
    @GetMapping("/transactions/recent")
    public ResponseEntity<List<GetRecentWalletTransactionsResponse>> getRecentWalletTransactions(
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        List<GetRecentWalletTransactionsResponse> response = walletService.getRecentWalletTransactions(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves monthly summary of income and spending for the current month.
     *
     * @param userId the authenticated user's ID from gateway
     * @return 200 OK with income, spending, and currency
     */
    @GetMapping("/stats/monthly-summary")
    public ResponseEntity<GetMonthlySummaryResponse> getMonthlySummary(
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        GetMonthlySummaryResponse response = walletService.getMonthlySummary(userId);
        return ResponseEntity.ok(response);
    }
}
