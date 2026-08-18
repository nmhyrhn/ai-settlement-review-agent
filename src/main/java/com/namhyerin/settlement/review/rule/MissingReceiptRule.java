package com.namhyerin.settlement.review.rule;

import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.domain.RuleCode;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MissingReceiptRule implements SettlementReviewRule {

    @Override
    public List<ReviewViolation> evaluate(SettlementTransaction target, List<SettlementTransaction> allTransactions) {
        if (target.hasReceipt()) {
            return List.of();
        }

        return List.of(new ReviewViolation(
                target.transactionId(),
                RuleCode.RECEIPT_MISSING,
                "증빙 번호가 등록되지 않았습니다."
        ));
    }
}

