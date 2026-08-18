package com.namhyerin.settlement.policy;

import com.namhyerin.settlement.policy.PolicyDocumentRepository.PolicySummary;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/policies")
public class PolicyDocumentController {

    private final PolicyDocumentService service;

    public PolicyDocumentController(PolicyDocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public PolicySummary register(@RequestParam @NotBlank String title,
                                  @RequestParam MultipartFile file,
                                  Authentication authentication) {
        return service.register(title, file, authentication.getName());
    }

    @GetMapping
    public List<PolicySummary> policies() {
        return service.findAll();
    }
}
