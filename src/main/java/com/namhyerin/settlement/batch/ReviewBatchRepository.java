package com.namhyerin.settlement.batch;

import com.namhyerin.settlement.review.domain.SettlementTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

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
}
