package com.apexpay.wallet_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for wallet transfer operations.
 *
 * @param message                   a success message confirming the transfer
 * @param recipientName             the username of the transfer recipient
 * @param payerTransactionReference the transaction reference for the payer's records
 * @param timestamp                 the timestamp when the transfer was processed
 * @param paymentMethod             the wallet name used for the transfer
 * @param amount                    the amount transferred
 */
public record TransferResponse(String message,
                               String recipientName,
                               String payerTransactionReference,
                               Instant timestamp,
                               // payment method for transfer should be the wallet name
                               String paymentMethod,
                               BigDecimal amount
) {
}
