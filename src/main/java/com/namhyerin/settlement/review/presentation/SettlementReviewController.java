package com.namhyerin.settlement.review.presentation;

import com.namhyerin.settlement.review.application.SettlementReviewService;
import com.namhyerin.settlement.review.application.CsvSettlementParser;
import com.namhyerin.settlement.review.application.ReviewDecisionService;
import com.namhyerin.settlement.review.application.ReviewDecisionService.DecisionHistory;
import com.namhyerin.settlement.review.application.ReviewDecisionService.DecisionStatus;
import com.namhyerin.settlement.review.application.SettlementReviewService.ReviewResult;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class SettlementReviewController {

    private final SettlementReviewService reviewService;
    private final CsvSettlementParser csvParser;
    private final ReviewDecisionService decisionService;

    public SettlementReviewController(SettlementReviewService reviewService, CsvSettlementParser csvParser,
                                      ReviewDecisionService decisionService) {
        this.reviewService = reviewService;
        this.csvParser = csvParser;
        this.decisionService = decisionService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<List<ReviewResult>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(reviewService.review(csvParser.parse(file)));
    }

    @PostMapping("/analyze")
    public ResponseEntity<List<ReviewResult>> analyze(@Valid @RequestBody AnalyzeRequest request) {
        List<SettlementTransaction> transactions = request.transactions().stream()
                .map(TransactionRequest::toDomain)
                .toList();
        return ResponseEntity.ok(reviewService.review(transactions));
    }

    @PostMapping("/{transactionId}/decisions")
    public ResponseEntity<DecisionHistory> decide(@PathVariable @NotBlank String transactionId,
                                                   @Valid @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(decisionService.decide(transactionId, request.status()));
    }

    @GetMapping("/{transactionId}/decisions")
    public ResponseEntity<List<DecisionHistory>> decisions(@PathVariable String transactionId) {
        return ResponseEntity.ok(decisionService.history(transactionId));
    }

    public record AnalyzeRequest(@NotEmpty List<@Valid TransactionRequest> transactions) {
    }

    public record DecisionRequest(@NotNull DecisionStatus status) {
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
