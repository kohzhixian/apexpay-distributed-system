package com.apexpay.wallet_service.dto.response;

/**
 * Response DTO for fund reservation cancellation.
 * <p>
 * Returned after successfully cancelling a reserved fund transaction,
 * releasing the funds back to available balance.
 * </p>
 *
 * @param message a success message confirming the reservation cancellation
 */
public record CancelReservationResponse(String message) {
}
