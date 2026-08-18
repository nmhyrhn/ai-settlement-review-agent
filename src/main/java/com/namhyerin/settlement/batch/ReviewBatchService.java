package com.namhyerin.settlement.batch;

import com.namhyerin.settlement.auth.AppUserRepository;
import com.namhyerin.settlement.review.application.CsvSettlementParser;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ReviewBatchService {

    private final AppUserRepository userRepository;
    private final ReviewBatchRepository batchRepository;
    private final CsvSettlementParser csvParser;

    public ReviewBatchService(AppUserRepository userRepository, ReviewBatchRepository batchRepository,
                              CsvSettlementParser csvParser) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.csvParser = csvParser;
    }

    @Transactional
    public BatchCreated create(String email, MultipartFile file) {
        var user = userRepository.findByEmail(email).orElseThrow();
        List<SettlementTransaction> transactions = csvParser.parse(file);
        long batchId = batchRepository.create(user.id(), file.getOriginalFilename());
        // CSV 파싱이 끝난 거래를 한 배치에 묶어 원문 필드만 저장함
        batchRepository.saveTransactions(batchId, transactions);
        return new BatchCreated(batchId, file.getOriginalFilename(), "PROCESSING", transactions.size());
    }

    public record BatchCreated(long batchId, String originalFilename, String status, int totalCount) {
    }
}
