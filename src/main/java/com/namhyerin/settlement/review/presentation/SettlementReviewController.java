package com.namhyerin.settlement.review.presentation;

import com.namhyerin.settlement.review.application.SettlementReviewService;
import com.namhyerin.settlement.review.application.SettlementReviewService.ReviewResult;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class SettlementReviewController {

    private final SettlementReviewService reviewService;

    public SettlementReviewController(SettlementReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<List<ReviewResult>> analyze(@Valid @RequestBody AnalyzeRequest request) {
        List<SettlementTransaction> transactions = request.transactions().stream()
                .map(TransactionRequest::toDomain)
                .toList();
        return ResponseEntity.ok(reviewService.review(transactions));
    }

    public record AnalyzeRequest(@NotEmpty List<@Valid TransactionRequest> transactions) {
    }

    public record TransactionRequest(
            @NotBlank String transactionId,
            @NotNull LocalDate transactionDate,
            @NotBlank String merchant,
            @NotNull @Positive BigDecimal amount,
            String receiptNumber
    ) {
        SettlementTransaction toDomain() {
            return new SettlementTransaction(transactionId, transactionDate, merchant, amount, receiptNumber);
        }
    }
}

