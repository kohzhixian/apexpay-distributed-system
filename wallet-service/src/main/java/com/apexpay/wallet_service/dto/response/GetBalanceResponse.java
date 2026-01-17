package com.apexpay.wallet_service.dto.response;

import java.math.BigDecimal;

public record GetBalanceResponse(BigDecimal balance) {
}
