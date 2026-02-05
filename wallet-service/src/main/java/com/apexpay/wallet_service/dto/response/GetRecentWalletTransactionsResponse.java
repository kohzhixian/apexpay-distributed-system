package com.apexpay.wallet_service.dto.response;

import com.apexpay.wallet_service.enums.WalletTransactionStatusEnum;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for recent wallet transactions.
 *
 * @param walletTransactionId the unique identifier of the transaction
 * @param datetime            the timestamp when the transaction occurred
 * @param description         a human-readable description of the transaction
 * @param walletName          the name of the wallet involved
 * @param amount              the transaction amount
 * @param isCredit            true if money was received, false if spent
 * @param status              the current status of the transaction
 */
public record GetRecentWalletTransactionsResponse(String walletTransactionId, Instant datetime, String description,
                                                  String walletName, BigDecimal amount,
                                                  Boolean isCredit, WalletTransactionStatusEnum status) {
}
