package com.namhyerin.settlement.batch;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class TransactionDecisionService {

    private final JdbcTemplate jdbcTemplate;

    public TransactionDecisionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public DecisionResult decide(long batchId, long transactionId, DecisionStatus status, String reason,
                                 String email, boolean admin) {
        DecisionTarget target = findTarget(batchId, transactionId, email, admin);
        // 과거 판단은 덮어쓰지 않고 담당자와 시각을 포함한 이력으로 추가함
        jdbcTemplate.update("""
                insert into transaction_decision(transaction_id, status, reason, decided_by)
                values (?, ?, ?, ?)
                """, target.transactionId(), status.name(), normalize(reason), target.userId());
        // 목록에서 현재 판단을 바로 표시할 수 있도록 거래의 최신 상태도 함께 갱신함
        jdbcTemplate.update("update settlement_transaction set review_status = ? where id = ?",
                status.name(), target.transactionId());
        return latest(target.transactionId());
    }

    private DecisionTarget findTarget(long batchId, long transactionId, String email, boolean admin) {
        return jdbcTemplate.query("""
                select t.id transaction_id, actor.id user_id
                from settlement_transaction t
                join review_batch b on b.id = t.batch_id
                join app_user owner on owner.id = b.created_by
                join app_user actor on actor.email = ?
                where b.id = ? and t.id = ? and (? = true or owner.email = ?)
                """, (result, row) -> new DecisionTarget(result.getLong("transaction_id"),
                        result.getLong("user_id")), email, batchId, transactionId, admin, email)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private DecisionResult latest(long transactionId) {
        return jdbcTemplate.query("""
                select d.id, d.transaction_id, d.status, d.reason, u.email decided_by, d.decided_at
                from transaction_decision d join app_user u on u.id = d.decided_by
                where d.transaction_id = ? order by d.id desc limit 1
                """, (result, row) -> new DecisionResult(result.getLong("id"),
                        result.getLong("transaction_id"), DecisionStatus.valueOf(result.getString("status")),
                        result.getString("reason"), result.getString("decided_by"),
                        result.getTimestamp("decided_at").toLocalDateTime()), transactionId).getFirst();
    }

    private String normalize(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    public enum DecisionStatus {
        APPROVED,
        RECHECK,
        HOLD
    }

    private record DecisionTarget(long transactionId, long userId) {
    }

    public record DecisionResult(long id, long transactionId, DecisionStatus status, String reason,
                                 String decidedBy, LocalDateTime decidedAt) {
    }
}
