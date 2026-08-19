package com.namhyerin.settlement.batch;

import com.namhyerin.settlement.review.domain.SettlementTransaction;
import com.namhyerin.settlement.review.domain.ReviewViolation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.namhyerin.settlement.policy.PolicyRagClient.ExplanationResponse;

@Repository
public class ReviewBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReviewBatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(long userId, String filename) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into review_batch(created_by, original_filename, status, rule_version)
                    values (?, ?, 'PROCESSING', '1.0')
                    """, new String[]{"id"});
            statement.setLong(1, userId);
            statement.setString(2, filename);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public void saveTransactions(long batchId, List<SettlementTransaction> transactions) {
        jdbcTemplate.batchUpdate("""
                insert into settlement_transaction
                (batch_id, external_transaction_id, transaction_date, merchant, amount, receipt_number)
                values (?, ?, ?, ?, ?, ?)
                """, transactions, transactions.size(), (statement, transaction) -> {
            statement.setLong(1, batchId);
            statement.setString(2, transaction.transactionId());
            statement.setObject(3, transaction.transactionDate());
            statement.setString(4, transaction.merchant());
            statement.setBigDecimal(5, transaction.amount());
            statement.setString(6, transaction.receiptNumber());
        });
        jdbcTemplate.update("update review_batch set total_count = ? where id = ?", transactions.size(), batchId);
    }

    public void saveViolations(long batchId, List<ReviewViolation> violations) {
        jdbcTemplate.batchUpdate("""
                insert into review_violation(transaction_id, rule_code, reason)
                select id, ?, ? from settlement_transaction
                where batch_id = ? and external_transaction_id = ?
                """, violations, violations.size(), (statement, violation) -> {
            statement.setString(1, violation.ruleCode().name());
            statement.setString(2, violation.reason());
            statement.setLong(3, batchId);
            statement.setString(4, violation.transactionId());
        });
    }

    public void complete(long batchId) {
        jdbcTemplate.update("update review_batch set status = 'COMPLETED' where id = ?", batchId);
    }

    public void saveExplanation(long batchId, String externalId, ExplanationResponse explanation,
                                ObjectMapper objectMapper) {
        try {
            jdbcTemplate.update("""
                    insert into review_ai_explanation(transaction_id, summary, citations_json, generated_by)
                    select id, ?, ?, ? from settlement_transaction
                    where batch_id = ? and external_transaction_id = ?
                    """, explanation.summary(), objectMapper.writeValueAsString(explanation.citations()),
                    explanation.generatedBy(), batchId, externalId);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 인용 정보를 저장하지 못함", exception);
        }
    }

    public Optional<BatchRow> findBatch(long batchId, String email, boolean admin) {
        return jdbcTemplate.query("""
                select b.id, b.original_filename, b.status, b.total_count, b.created_at
                from review_batch b join app_user u on u.id = b.created_by
                where b.id = ? and (? = true or u.email = ?)
                """, (result, row) -> new BatchRow(result.getLong("id"), result.getString("original_filename"),
                        result.getString("status"), result.getInt("total_count"),
                        result.getTimestamp("created_at").toLocalDateTime()), batchId, admin, email)
                .stream().findFirst();
    }

    public List<TransactionRow> findTransactions(long batchId) {
        return jdbcTemplate.query("""
                select id, external_transaction_id, transaction_date, merchant, amount,
                       receipt_number, review_status
                from settlement_transaction where batch_id = ? order by id
                """, (result, row) -> new TransactionRow(result.getLong("id"),
                        result.getString("external_transaction_id"), result.getObject("transaction_date", LocalDate.class),
                        result.getString("merchant"), result.getBigDecimal("amount"),
                        result.getString("receipt_number"), result.getString("review_status")), batchId);
    }

    public List<ViolationRow> findViolations(long transactionId) {
        return jdbcTemplate.query("select rule_code, reason from review_violation where transaction_id = ? order by id",
                (result, row) -> new ViolationRow(result.getString("rule_code"), result.getString("reason")),
                transactionId);
    }

    public Optional<ExplanationRow> findExplanation(long transactionId) {
        return jdbcTemplate.query("""
                select summary, citations_json, generated_by from review_ai_explanation where transaction_id = ?
                """, (result, row) -> new ExplanationRow(result.getString("summary"),
                        result.getString("citations_json"), result.getString("generated_by")), transactionId)
                .stream().findFirst();
    }

    public record BatchRow(long id, String originalFilename, String status, int totalCount,
                           LocalDateTime createdAt) {
    }

    public record TransactionRow(long id, String transactionId, LocalDate transactionDate, String merchant,
                                 BigDecimal amount, String receiptNumber, String reviewStatus) {
    }

    public record ViolationRow(String ruleCode, String reason) {
    }

    public record ExplanationRow(String summary, String citationsJson, String generatedBy) {
    }
}
