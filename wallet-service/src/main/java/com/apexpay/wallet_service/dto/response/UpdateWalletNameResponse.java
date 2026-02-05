package com.apexpay.wallet_service.dto.response;

/**
 * Response DTO for wallet name update operations.
 *
 * @param message a success message confirming the name change
 */
public record UpdateWalletNameResponse(String message) {
}
