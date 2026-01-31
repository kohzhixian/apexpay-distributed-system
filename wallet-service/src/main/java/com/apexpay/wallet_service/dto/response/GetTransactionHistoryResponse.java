package com.apexpay.wallet_service.dto.response;

import com.apexpay.wallet_service.enums.ReferenceTypeEnum;
import com.apexpay.wallet_service.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for wallet transaction history entries.
 * <p>
 * Represents a single transaction in the wallet's transaction history,
 * including amount, type, and references to related entities.
 * </p>
 *
 * @param transactionId        the wallet transaction ID
 * @param transactionReference human-readable reference for customer support (e.g., APX-8921-MNQ-772)
 * @param amount               the transaction amount
 * @param transactionType      the type of transaction (CREDIT/DEBIT)
 * @param referenceId          the ID of the related entity (e.g., payment ID, transfer wallet ID)
 * @param referenceType        the type of reference (PAYMENT/TRANSFER/TOPUP)
 */
public record GetTransactionHistoryResponse(
        UUID transactionId,
        String transactionReference,
        BigDecimal amount,
        TransactionTypeEnum transactionType,
        String referenceId,
        ReferenceTypeEnum referenceType
) {
}
