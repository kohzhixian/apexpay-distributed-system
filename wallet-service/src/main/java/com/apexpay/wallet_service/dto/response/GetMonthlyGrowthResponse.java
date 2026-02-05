package com.apexpay.wallet_service.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for monthly growth percentage.
 * <p>
 * Returns the percentage growth comparing current total balance
 * against last month's ending balance. Calculated as:
 * ((currentBalance - lastMonthBalance) / lastMonthBalance) * 100
 * </p>
 *
 * @param monthlyGrowth the growth percentage (can be negative for decline)
 */
public record GetMonthlyGrowthResponse(BigDecimal monthlyGrowth) {
}
