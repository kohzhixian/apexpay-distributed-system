package com.apexpay.wallet_service.dto.response;

import java.time.Instant;

/**
 * Response DTO for wallet transfer operations.
 *
 * @param message a success message confirming the transfer
 */
public record TransferResponse(String message,
                               String recipientName,
                               String payerTransactionReference,
                               Instant timestamp,
                               // payment method for transfer should be the wallet name
                               String paymentMethod
) {
}
