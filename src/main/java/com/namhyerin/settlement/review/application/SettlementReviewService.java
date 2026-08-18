package com.namhyerin.settlement.review.application;

import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import com.namhyerin.settlement.review.rule.SettlementReviewRule;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettlementReviewService {

    private final List<SettlementReviewRule> rules;
    private final AiReviewExplainer explainer;

    public SettlementReviewService(List<SettlementReviewRule> rules, AiReviewExplainer explainer) {
        this.rules = rules;
        this.explainer = explainer;
    }

    public List<ReviewResult> review(List<SettlementTransaction> transactions) {
        return transactions.stream()
                .map(transaction -> reviewOne(transaction, transactions))
                .toList();
    }

    private ReviewResult reviewOne(SettlementTransaction transaction, List<SettlementTransaction> allTransactions) {
        List<ReviewViolation> violations = rules.stream()
                .flatMap(rule -> rule.evaluate(transaction, allTransactions).stream())
                .toList();
        return new ReviewResult(transaction, violations, explainer.explain(transaction, violations));
    }

    public record ReviewResult(
            SettlementTransaction transaction,
            List<ReviewViolation> violations,
            AiReviewExplanation explanation
    ) {
    }
}
