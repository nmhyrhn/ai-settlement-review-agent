package com.namhyerin.settlement.batch;

import com.namhyerin.settlement.auth.AppUserRepository;
import com.namhyerin.settlement.review.application.CsvSettlementParser;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.application.SettlementReviewService;
import com.namhyerin.settlement.policy.PolicyRagClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ReviewBatchService {

    private final AppUserRepository userRepository;
    private final ReviewBatchRepository batchRepository;
    private final CsvSettlementParser csvParser;
    private final SettlementReviewService reviewService;
    private final PolicyRagClient ragClient;
    private final ObjectMapper objectMapper;

    public ReviewBatchService(AppUserRepository userRepository, ReviewBatchRepository batchRepository,
                              CsvSettlementParser csvParser, SettlementReviewService reviewService,
                              PolicyRagClient ragClient, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.csvParser = csvParser;
        this.reviewService = reviewService;
        this.ragClient = ragClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BatchCreated create(String email, MultipartFile file) {
        var user = userRepository.findByEmail(email).orElseThrow();
        List<SettlementTransaction> transactions = csvParser.parse(file);
        long batchId = batchRepository.create(user.id(), file.getOriginalFilename());
        // CSV 파싱이 끝난 거래를 한 배치에 묶어 원문 필드만 저장함
        batchRepository.saveTransactions(batchId, transactions);
        List<ReviewViolation> violations = transactions.stream()
                .flatMap(transaction -> reviewService.evaluate(transaction, transactions).stream())
                .toList();
        // AI 호출 전에 확정 가능한 Java 규칙 위반 결과를 먼저 영구 저장함
        batchRepository.saveViolations(batchId, violations);
        transactions.forEach(transaction -> {
            List<ReviewViolation> transactionViolations = violations.stream()
                    .filter(violation -> violation.transactionId().equals(transaction.transactionId())).toList();
            if (!transactionViolations.isEmpty()) {
                batchRepository.saveExplanation(batchId, transaction.transactionId(),
                        explain(transaction, transactionViolations), objectMapper);
            }
        });
        batchRepository.complete(batchId);
        int explanationCount = (int) violations.stream().map(ReviewViolation::transactionId).distinct().count();
        return new BatchCreated(batchId, file.getOriginalFilename(), "COMPLETED", transactions.size(),
                violations.size(), explanationCount);
    }

    private PolicyRagClient.ExplanationResponse explain(SettlementTransaction transaction,
                                                        List<ReviewViolation> violations) {
        try {
            return ragClient.explain(transaction, violations);
        } catch (RuntimeException exception) {
            // RAG 장애가 발생해도 확정된 규칙 위반 결과와 배치 처리를 유지함
            String summary = violations.stream().map(ReviewViolation::reason).distinct()
                    .reduce((left, right) -> left + " " + right).orElse("규칙 위반을 확인해야 함");
            return new PolicyRagClient.ExplanationResponse(summary, List.<Map<String, Object>>of(),
                    "RULE_FALLBACK");
        }
    }

    public BatchDetail detail(long batchId, String email, boolean admin) {
        var batch = batchRepository.findBatch(batchId, email, admin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<TransactionDetail> transactions = batchRepository.findTransactions(batchId).stream()
                .map(transaction -> new TransactionDetail(transaction.id(), transaction.transactionId(),
                        transaction.transactionDate(), transaction.merchant(), transaction.amount(),
                        transaction.receiptNumber(), transaction.reviewStatus(),
                        batchRepository.findViolations(transaction.id()), explanation(transaction.id())))
                .toList();
        return new BatchDetail(batch.id(), batch.originalFilename(), batch.status(), batch.totalCount(),
                batch.createdAt(), transactions);
    }

    private ExplanationDetail explanation(long transactionId) {
        return batchRepository.findExplanation(transactionId).map(explanation -> {
            try {
                List<Map<String, Object>> citations = objectMapper.readValue(explanation.citationsJson(),
                        new TypeReference<>() {});
                return new ExplanationDetail(explanation.summary(), citations, explanation.generatedBy());
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw new IllegalStateException("AI 인용 정보를 읽지 못함", exception);
            }
        }).orElse(null);
    }

    public record BatchCreated(long batchId, String originalFilename, String status, int totalCount,
                               int violationCount, int explanationCount) {
    }

    public record BatchDetail(long batchId, String originalFilename, String status, int totalCount,
                              java.time.LocalDateTime createdAt, List<TransactionDetail> transactions) {
    }

    public record TransactionDetail(long id, String transactionId, java.time.LocalDate transactionDate,
                                    String merchant, java.math.BigDecimal amount, String receiptNumber,
                                    String reviewStatus, List<ReviewBatchRepository.ViolationRow> violations,
                                    ExplanationDetail explanation) {
    }

    public record ExplanationDetail(String summary, List<Map<String, Object>> citations, String generatedBy) {
    }
}
