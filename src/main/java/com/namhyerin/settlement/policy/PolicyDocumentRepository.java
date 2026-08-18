package com.namhyerin.settlement.policy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class PolicyDocumentRepository {

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public PolicyDocumentRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public int nextVersion(String title) {
        return jdbcClient.sql("select coalesce(max(version_no), 0) + 1 from policy_document where title = ?")
                .param(title)
                .query(Integer.class)
                .single();
    }

    public long create(String title, int version, String filename, String contentType,
                       byte[] content, String registeredBy) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into policy_document
                    (title, version_no, original_filename, content_type, content, status, registered_by)
                    values (?, ?, ?, ?, ?, 'PROCESSING', ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setInt(2, version);
            statement.setString(3, filename);
            statement.setString(4, contentType);
            statement.setBytes(5, content);
            statement.setString(6, registeredBy);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void activate(long id) {
        // Python 문서 처리가 성공한 정책만 검색 대상으로 활성화함
        jdbcClient.sql("update policy_document set status = 'ACTIVE' where id = ?")
                .param(id)
                .update();
    }

    public void fail(long id) {
        jdbcClient.sql("update policy_document set status = 'FAILED' where id = ?")
                .param(id)
                .update();
    }

    public List<PolicySummary> findAll() {
        return jdbcClient.sql("""
                        select id, title, version_no, original_filename, status, registered_by, created_at
                        from policy_document order by created_at desc, id desc
                        """)
                .query(PolicySummary.class)
                .list();
    }

    public record PolicySummary(long id, String title, int versionNo, String originalFilename,
                                String status, String registeredBy, java.time.LocalDateTime createdAt) {
    }
}
