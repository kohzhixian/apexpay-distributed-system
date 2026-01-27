package com.apexpay.common.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for fund reservation operations.
 * <p>
 * Returned after successfully reserving funds in a wallet. Contains the
 * transaction ID for the reservation and the updated wallet balance.
 * </p>
 *
 * @param walletTransactionId the ID of the created wallet transaction (PENDING status)
 * @param walletId           the wallet ID where funds were reserved
 * @param amountReserved     the amount that was reserved
 * @param remainingBalance   the wallet's remaining available balance after reservation
 */
public record ReserveFundsResponse(UUID walletTransactionId, UUID walletId, BigDecimal amountReserved,
                                   BigDecimal remainingBalance) {
}
