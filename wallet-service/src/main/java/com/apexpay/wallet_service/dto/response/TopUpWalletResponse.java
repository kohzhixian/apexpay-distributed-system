package com.apexpay.wallet_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for wallet top-up operations.
 *
 * @param message              a success message confirming the top-up
 * @param transactionId        the unique ID of the wallet transaction
 * @param transactionReference human-readable reference for customer support (e.g., APX-8921-MNQ-772)
 * @param amount               the amount topped up
 * @param newBalance           the wallet's new balance after top-up
 */
public record TopUpWalletResponse(
        String message,
        UUID transactionId,
        String transactionReference,
        BigDecimal amount,
        BigDecimal newBalance
) {
}
