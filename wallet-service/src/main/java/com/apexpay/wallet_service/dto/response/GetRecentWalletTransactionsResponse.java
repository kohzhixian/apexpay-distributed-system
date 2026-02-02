package com.apexpay.wallet_service.dto.response;

import com.apexpay.wallet_service.enums.WalletTransactionStatusEnum;

import java.math.BigDecimal;
import java.time.Instant;

public record GetRecentWalletTransactionsResponse(String walletTransactionId, Instant datetime, String description,
                                                  String walletName, BigDecimal amount,
                                                  Boolean isCredit, WalletTransactionStatusEnum status) {
}
