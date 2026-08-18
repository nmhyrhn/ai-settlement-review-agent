package com.namhyerin.settlement.policy;

import com.namhyerin.settlement.policy.PolicyDocumentRepository.PolicySummary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PolicyDocumentService {

    private static final Logger log = LoggerFactory.getLogger(PolicyDocumentService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "md", "txt");
    private final PolicyDocumentRepository repository;
    private final PolicyRagClient ragClient;

    public PolicyDocumentService(PolicyDocumentRepository repository, PolicyRagClient ragClient) {
        this.repository = repository;
        this.ragClient = ragClient;
    }

    public PolicySummary register(String title, MultipartFile file, String registeredBy) {
        validate(file);
        try {
            byte[] content = file.getBytes();
            int version = repository.nextVersion(title);
            long id = repository.create(title.trim(), version, file.getOriginalFilename(),
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                    content, registeredBy);
            try {
                ragClient.index(id, title.trim(), version, file.getOriginalFilename(), file.getContentType(), content);
                repository.activate(id);
            } catch (RuntimeException exception) {
                repository.fail(id);
                log.warn("정책 문서 인덱싱에 실패함: documentId={}", id, exception);
                throw new PolicyIndexException("정책 문서 처리 서비스에 연결하지 못했습니다.");
            }
            return repository.findAll().stream().filter(policy -> policy.id() == id).findFirst().orElseThrow();
        } catch (IOException exception) {
            throw new IllegalArgumentException("정책 문서를 읽지 못했습니다.");
        }
    }

    public List<PolicySummary> findAll() {
        return repository.findAll();
    }

    private static void validate(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("정책 문서 파일이 필요합니다.");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("PDF, MD, TXT 파일만 등록할 수 있습니다.");
        }
    }

    public static class PolicyIndexException extends RuntimeException {
        public PolicyIndexException(String message) {
            super(message);
        }
    }
}
