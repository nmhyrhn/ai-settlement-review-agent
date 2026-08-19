package com.namhyerin.settlement.batch;

import com.namhyerin.settlement.batch.ReviewBatchService.BatchCreated;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/review-batches")
public class ReviewBatchController {

    private final ReviewBatchService service;

    public ReviewBatchController(ReviewBatchService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public BatchCreated create(@RequestParam MultipartFile file, Authentication authentication) {
        return service.create(authentication.getName(), file);
    }

    @GetMapping("/{batchId}")
    public ReviewBatchService.BatchDetail detail(@PathVariable long batchId, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return service.detail(batchId, authentication.getName(), admin);
    }
}
