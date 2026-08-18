package com.namhyerin.settlement.review.rule;

import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.domain.SettlementTransaction;

import java.util.List;

public interface SettlementReviewRule {
    List<ReviewViolation> evaluate(SettlementTransaction target, List<SettlementTransaction> allTransactions);
}

