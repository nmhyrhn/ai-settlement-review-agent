package com.namhyerin.settlement.review.rule;

import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.domain.RuleCode;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DuplicatePaymentRule implements SettlementReviewRule {

    @Override
    public List<ReviewViolation> evaluate(SettlementTransaction target, List<SettlementTransaction> allTransactions) {
        long samePaymentCount = allTransactions.stream()
                .filter(transaction -> samePayment(target, transaction))
                .count();

        if (samePaymentCount < 2) {
            return List.of();
        }

        return List.of(new ReviewViolation(
                target.transactionId(),
                RuleCode.DUPLICATE_PAYMENT,
                "같은 날짜, 거래처, 금액의 결제가 두 건 이상 존재합니다."
        ));
    }

    private boolean samePayment(SettlementTransaction first, SettlementTransaction second) {
        return first.transactionDate().equals(second.transactionDate())
                && first.merchant().equalsIgnoreCase(second.merchant())
                && first.amount().compareTo(second.amount()) == 0;
    }
}

