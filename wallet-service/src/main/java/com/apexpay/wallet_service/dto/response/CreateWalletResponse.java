package com.apexpay.wallet_service.dto.response;

import java.util.UUID;

/**
 * Response DTO for wallet creation.
 *
 * @param message    a success message confirming wallet creation
 * @param walletId   the ID of the newly created wallet
 * @param walletName the user-defined name of the wallet
 */
public record CreateWalletResponse(String message, UUID walletId, String walletName) {
}
