package com.namhyerin.settlement.batch;

import com.namhyerin.settlement.auth.AppUserRepository;
import com.namhyerin.settlement.review.application.CsvSettlementParser;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.application.SettlementReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ReviewBatchService {

    private final AppUserRepository userRepository;
    private final ReviewBatchRepository batchRepository;
    private final CsvSettlementParser csvParser;
    private final SettlementReviewService reviewService;

    public ReviewBatchService(AppUserRepository userRepository, ReviewBatchRepository batchRepository,
                              CsvSettlementParser csvParser, SettlementReviewService reviewService) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.csvParser = csvParser;
        this.reviewService = reviewService;
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
        batchRepository.complete(batchId);
        return new BatchCreated(batchId, file.getOriginalFilename(), "COMPLETED", transactions.size(), violations.size());
    }

    public record BatchCreated(long batchId, String originalFilename, String status, int totalCount, int violationCount) {
    }
}
