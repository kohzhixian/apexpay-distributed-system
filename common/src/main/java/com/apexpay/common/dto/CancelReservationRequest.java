package com.apexpay.common.dto;

import com.apexpay.common.constants.ValidationMessages;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for cancelling a fund reservation.
 * <p>
 * Used in the rollback phase of the two-phase commit pattern when payment
 * processing fails. Releases the reserved funds back to available balance
 * and marks the wallet transaction as CANCELLED.
 * </p>
 *
 * @param walletTransactionId the wallet transaction ID of the reservation to cancel
 */
public record CancelReservationRequest(
        @NotNull(message = ValidationMessages.RESERVATION_ID_REQUIRED)
        UUID walletTransactionId
) {
}
