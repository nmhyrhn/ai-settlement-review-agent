package com.namhyerin.settlement.review.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ReviewDecisionService {

    private final JdbcTemplate jdbcTemplate;

    public ReviewDecisionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DecisionHistory decide(String transactionId, DecisionStatus status) {
        DecisionHistory decision = new DecisionHistory(transactionId, status, Instant.now());
        jdbcTemplate.update("insert into review_decision(transaction_id, status, decided_at) values (?, ?, ?)",
                transactionId, status.name(), decision.decidedAt().toString());
        return decision;
    }

    public List<DecisionHistory> history(String transactionId) {
        return jdbcTemplate.query("""
                        select transaction_id, status, decided_at
                        from review_decision
                        where transaction_id = ?
                        order by id
                        """,
                (resultSet, rowNumber) -> new DecisionHistory(
                        resultSet.getString("transaction_id"),
                        DecisionStatus.valueOf(resultSet.getString("status")),
                        Instant.parse(resultSet.getString("decided_at"))),
                transactionId);
    }

    public enum DecisionStatus {
        APPROVE,
        RECHECK,
        HOLD
    }

    public record DecisionHistory(String transactionId, DecisionStatus status, Instant decidedAt) {
    }
}
