package com.apexpay.payment_service.repository;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.common.dto.CancelReservationRequest;
import com.apexpay.common.dto.ConfirmReservationRequest;
import com.apexpay.common.dto.ReserveFundsRequest;
import com.apexpay.common.dto.ReserveFundsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Feign client for interacting with the wallet service.
 * <p>
 * Provides methods for managing wallet fund reservations as part of the
 * two-phase commit pattern for payment processing. All methods require
 * the X-USER-ID header for authentication.
 * </p>
 */
@FeignClient(value = "walletservice", url = "${apexpay.services.wallet-service-url:}")
public interface WalletClient {

    /**
     * Reserves funds in a wallet for a pending payment.
     * <p>
     * Moves the specified amount from available balance to reserved balance,
     * creating a PENDING wallet transaction. This is the first phase of the
     * two-phase commit pattern.
     * </p>
     *
     * @param request the reservation request containing amount, currency, and paymentId
     * @param userId  the authenticated user's ID
     * @param walletId the wallet ID to reserve funds from
     * @return response containing walletTransactionId and remaining balance
     */
    @PostMapping("/api/v1/wallet/{walletId}/reserve")
    ReserveFundsResponse reserveFunds(@RequestBody ReserveFundsRequest request, @RequestHeader(HttpHeaders.X_USER_ID) String userId,
                                      @PathVariable("walletId") UUID walletId);

    /**
     * Confirms a fund reservation after successful payment provider charge.
     * <p>
     * Moves reserved funds to completed state, deducts from balance, and updates
     * the wallet transaction to COMPLETED status. This is the commit phase of
     * the two-phase commit pattern.
     * </p>
     *
     * @param request the confirmation request containing walletTransactionId,
     *                providerTransactionId, and provider name
     * @param userId  the authenticated user's ID
     * @param walletId the wallet ID containing the reservation
     */
    @PostMapping("/api/v1/wallet/{walletId}/confirm")
    void confirmReservation(@RequestBody ConfirmReservationRequest request, @RequestHeader(HttpHeaders.X_USER_ID) String userId,
                            @PathVariable("walletId") UUID walletId);

    /**
     * Cancels a fund reservation when payment fails.
     * <p>
     * Releases reserved funds back to available balance and updates the wallet
     * transaction to CANCELLED status. This is the rollback phase of the
     * two-phase commit pattern.
     * </p>
     *
     * @param request the cancellation request containing walletTransactionId
     * @param userId  the authenticated user's ID
     * @param walletId the wallet ID containing the reservation
     */
    @PostMapping("/api/v1/wallet/{walletId}/cancel")
    void cancelReservation(@RequestBody CancelReservationRequest request, @RequestHeader(HttpHeaders.X_USER_ID) String userId,
                           @PathVariable("walletId") UUID walletId);

}
