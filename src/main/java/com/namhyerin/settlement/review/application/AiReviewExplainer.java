package com.namhyerin.settlement.review.application;

import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.domain.SettlementTransaction;

import java.util.List;

public interface AiReviewExplainer {
    AiReviewExplanation explain(SettlementTransaction transaction, List<ReviewViolation> violations);
}
