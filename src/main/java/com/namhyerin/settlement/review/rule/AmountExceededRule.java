package com.namhyerin.settlement.review.rule;

import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.domain.RuleCode;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AmountExceededRule implements SettlementReviewRule {

    private static final BigDecimal LIMIT = new BigDecimal("1000000");

    @Override
    public List<ReviewViolation> evaluate(SettlementTransaction target, List<SettlementTransaction> allTransactions) {
        // 기준 금액과 같은 거래는 정상이며 초과한 경우만 위반임
        if (target.amount().compareTo(LIMIT) <= 0) {
            return List.of();
        }

        return List.of(new ReviewViolation(
                target.transactionId(),
                RuleCode.AMOUNT_EXCEEDED,
                "결제 금액이 검수 기준 1,000,000원을 초과했습니다."
        ));
    }
}
