package com.apexpay.wallet_service.dto.response;

import com.apexpay.wallet_service.enums.ReferenceTypeEnum;
import com.apexpay.wallet_service.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.util.UUID;

public record GetTransactionHistoryResponse(UUID transactionId, BigDecimal amount,
                                            TransactionTypeEnum transactionType, String referenceId,
                                            ReferenceTypeEnum referenceType) {
}
