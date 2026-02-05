package com.apexpay.wallet_service.dto.response;

import java.math.BigDecimal;

import com.apexpay.common.enums.CurrencyEnum;

/**
 * Response DTO for monthly wallet summary.
 * <p>
 * Returns the total income (CREDIT) and spending (DEBIT) for the current month
 * across all user wallets. Only includes COMPLETED transactions.
 * </p>
 *
 * @param income   total credited amount for the month
 * @param spending total debited amount for the month
 * @param currency the currency of the amounts
 */
public record GetMonthlySummaryResponse(
                BigDecimal income,
                BigDecimal spending,
                CurrencyEnum currency) {
}
