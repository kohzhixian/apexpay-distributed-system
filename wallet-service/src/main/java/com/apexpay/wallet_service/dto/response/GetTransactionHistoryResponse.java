package com.apexpay.wallet_service.dto.response;

import com.apexpay.wallet_service.enums.ReferenceTypeEnum;
import com.apexpay.wallet_service.enums.TransactionTypeEnum;
import com.apexpay.wallet_service.enums.WalletTransactionStatusEnum;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for wallet transaction history entries.
 * <p>
 * Represents a single transaction in the wallet's transaction history,
 * including amount, type, wallet info, and references to related entities.
 * </p>
 *
 * @param transactionId        the wallet transaction ID
 * @param transactionReference human-readable reference for customer support (e.g., APX-8921-MNQ-772)
 * @param amount               the transaction amount
 * @param currency             the wallet currency (e.g., SGD, USD)
 * @param transactionType      the type of transaction (CREDIT/DEBIT)
 * @param referenceType        the type of reference (PAYMENT/TRANSFER/TOPUP)
 * @param referenceId          the ID of the related entity (e.g., payment ID, transfer wallet ID)
 * @param status               the transaction status (PENDING/COMPLETED/CANCELLED)
 * @param walletId             the wallet ID this transaction belongs to
 * @param walletName           the wallet name for display
 * @param createdAt            when the transaction was created
 * @param description          human-readable description of the transaction
 */
public record GetTransactionHistoryResponse(
        UUID transactionId,
        String transactionReference,
        BigDecimal amount,
        String currency,
        TransactionTypeEnum transactionType,
        ReferenceTypeEnum referenceType,
        String referenceId,
        WalletTransactionStatusEnum status,
        UUID walletId,
        String walletName,
        Instant createdAt,
        String description
) {
}
