package com.namhyerin.settlement.review.infrastructure;

import com.namhyerin.settlement.review.application.AiReviewExplainer;
import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FallbackReviewExplainer implements AiReviewExplainer {

    @Override
    public String explain(SettlementTransaction transaction, List<ReviewViolation> violations) {
        if (violations.isEmpty()) {
            return "자동 검수에서 확인이 필요한 항목이 발견되지 않았습니다.";
        }

        String reasons = violations.stream()
                .map(ReviewViolation::reason)
                .collect(Collectors.joining(" "));
        return reasons + " 담당자가 원본 내역과 증빙을 확인해 주세요.";
    }
}

