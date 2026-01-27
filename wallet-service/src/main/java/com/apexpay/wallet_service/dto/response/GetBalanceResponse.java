package com.apexpay.wallet_service.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for wallet balance queries.
 * <p>
 * Returns the current available balance of the wallet. Reserved funds
 * are not included in this balance.
 * </p>
 *
 * @param balance the current available wallet balance
 */
public record GetBalanceResponse(BigDecimal balance) {
}
