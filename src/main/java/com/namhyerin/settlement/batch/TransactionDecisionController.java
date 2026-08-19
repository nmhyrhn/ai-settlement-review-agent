package com.namhyerin.settlement.batch;

import com.namhyerin.settlement.batch.TransactionDecisionService.DecisionResult;
import com.namhyerin.settlement.batch.TransactionDecisionService.DecisionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-batches/{batchId}/transactions/{transactionId}/decisions")
public class TransactionDecisionController {

    private final TransactionDecisionService service;

    public TransactionDecisionController(TransactionDecisionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionResult decide(@PathVariable long batchId, @PathVariable long transactionId,
                                 @Valid @RequestBody DecisionRequest request, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return service.decide(batchId, transactionId, request.status(), request.reason(),
                authentication.getName(), admin);
    }

    public record DecisionRequest(@NotNull DecisionStatus status, @Size(max = 500) String reason) {
    }
}
