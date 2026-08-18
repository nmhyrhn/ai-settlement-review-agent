package com.namhyerin.settlement.review.application;

import com.namhyerin.settlement.review.domain.RuleCode;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import com.namhyerin.settlement.review.infrastructure.FallbackReviewExplainer;
import com.namhyerin.settlement.review.rule.AmountExceededRule;
import com.namhyerin.settlement.review.rule.DuplicatePaymentRule;
import com.namhyerin.settlement.review.rule.MissingReceiptRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementReviewServiceTest {

    private final SettlementReviewService service = new SettlementReviewService(
            List.of(new AmountExceededRule(), new MissingReceiptRule(), new DuplicatePaymentRule()),
            new FallbackReviewExplainer()
    );

    @Test
    void detectsAmountReceiptAndDuplicateViolations() {
        SettlementTransaction first = transaction("T-001", "1250000", null);
        SettlementTransaction second = transaction("T-002", "1250000", "R-002");

        List<SettlementReviewService.ReviewResult> results = service.review(List.of(first, second));

        assertThat(results.getFirst().violations())
                .extracting(violation -> violation.ruleCode())
                .containsExactlyInAnyOrder(
                        RuleCode.AMOUNT_EXCEEDED,
                        RuleCode.RECEIPT_MISSING,
                        RuleCode.DUPLICATE_PAYMENT
                );
        assertThat(results.get(1).violations())
                .extracting(violation -> violation.ruleCode())
                .containsExactlyInAnyOrder(RuleCode.AMOUNT_EXCEEDED, RuleCode.DUPLICATE_PAYMENT);
    }

    @Test
    void returnsNoViolationForNormalTransaction() {
        SettlementTransaction normal = transaction("T-003", "45000", "R-003");

        SettlementReviewService.ReviewResult result = service.review(List.of(normal)).getFirst();

        assertThat(result.violations()).isEmpty();
        assertThat(result.explanation().summary()).contains("발견되지 않았습니다");
    }

    private SettlementTransaction transaction(String id, String amount, String receiptNumber) {
        return new SettlementTransaction(
                id,
                LocalDate.of(2026, 8, 18),
                "ABC상사",
                new BigDecimal(amount),
                receiptNumber
        );
    }
}
