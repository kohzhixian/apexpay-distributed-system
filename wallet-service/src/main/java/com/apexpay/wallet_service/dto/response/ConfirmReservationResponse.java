package com.apexpay.wallet_service.dto.response;

/**
 * Response DTO for fund reservation confirmation.
 * <p>
 * Returned after successfully confirming a reserved fund transaction
 * as part of the two-phase commit pattern.
 * </p>
 *
 * @param message a success message confirming the reservation completion
 */
public record ConfirmReservationResponse(String message) {
}
