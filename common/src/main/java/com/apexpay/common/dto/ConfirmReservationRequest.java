package com.apexpay.common.dto;

import com.apexpay.common.constants.ValidationMessages;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for confirming a fund reservation.
 * <p>
 * Used in the commit phase of the two-phase commit pattern. Confirms that
 * the payment provider successfully charged the payment method, and moves
 * the reserved funds from reserved balance to completed transaction.
 * </p>
 *
 * @param walletTransactionId  the wallet transaction ID of the reservation to confirm
 * @param externalTransactionId the provider's transaction identifier from the successful charge
 * @param externalProvider     the name of the payment provider (e.g., "STRIPE", "MOCK")
 */
public record ConfirmReservationRequest(
        @NotNull(message = ValidationMessages.RESERVATION_ID_REQUIRED)
        UUID walletTransactionId,

        @NotNull(message = ValidationMessages.EXTERNAL_TRANSACTION_ID_REQUIRED)
        String externalTransactionId,

        @NotNull(message = ValidationMessages.EXTERNAL_PROVIDER_REQUIRED)
        String externalProvider
) {
}
