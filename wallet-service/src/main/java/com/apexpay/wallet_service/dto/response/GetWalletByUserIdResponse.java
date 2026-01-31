package com.apexpay.wallet_service.dto.response;

import com.apexpay.common.enums.CurrencyEnum;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for retrieving wallets by user ID.
 *
 * @param walletId the wallet's unique identifier
 * @param name     the user-defined name of the wallet
 * @param balance  the current wallet balance
 * @param currency the wallet's currency
 */
public record GetWalletByUserIdResponse(UUID walletId, String name, BigDecimal balance, CurrencyEnum currency) {
}
