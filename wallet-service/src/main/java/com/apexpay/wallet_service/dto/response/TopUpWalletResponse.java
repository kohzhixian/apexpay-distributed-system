package com.apexpay.wallet_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for wallet top-up operations.
 *
 * @param message       a success message confirming the top-up
 * @param transactionId the unique ID of the wallet transaction
 * @param amount        the amount topped up
 * @param newBalance    the wallet's new balance after top-up
 * @param createdAt     the timestamp when the transaction was created
 */
public record TopUpWalletResponse(
        String message,
        UUID transactionId,
        BigDecimal amount,
        BigDecimal newBalance,
        Instant createdAt
) {
}
