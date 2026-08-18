package com.namhyerin.settlement.review.domain;

public record ReviewViolation(
        String transactionId,
        RuleCode ruleCode,
        String reason
) {
}

