package com.namhyerin.settlement.review.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementTransaction(
        String transactionId,
        LocalDate transactionDate,
        String merchant,
        BigDecimal amount,
        String receiptNumber
) {
    public boolean hasReceipt() {
        return receiptNumber != null && !receiptNumber.isBlank();
    }
}

