package com.namhyerin.settlement.batch;

import com.namhyerin.settlement.auth.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:decision-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class TransactionDecisionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    long batchId;
    long transactionId;

    @BeforeEach
    void prepareBatch() {
        jdbcTemplate.update("delete from transaction_decision");
        jdbcTemplate.update("delete from review_ai_explanation");
        jdbcTemplate.update("delete from review_violation");
        jdbcTemplate.update("delete from settlement_transaction");
        jdbcTemplate.update("delete from review_batch");
        if (userRepository.findByEmail("owner@example.com").isEmpty()) {
            userRepository.create("owner@example.com", passwordEncoder.encode("password123"));
        }
        long ownerId = userRepository.findByEmail("owner@example.com").orElseThrow().id();
        jdbcTemplate.update("""
                insert into review_batch(created_by, original_filename, status, rule_version, total_count)
                values (?, 'decision.csv', 'COMPLETED', '1.0', 1)
                """, ownerId);
        batchId = jdbcTemplate.queryForObject("select max(id) from review_batch", Long.class);
        jdbcTemplate.update("""
                insert into settlement_transaction
                (batch_id, external_transaction_id, transaction_date, merchant, amount, review_status)
                values (?, 'D-001', '2026-08-19', '판단상사', 10000, 'PENDING')
                """, batchId);
        transactionId = jdbcTemplate.queryForObject("select max(id) from settlement_transaction", Long.class);
    }

    @Test
    void ownerAddsDecisionHistoryAndUpdatesCurrentStatus() throws Exception {
        mockMvc.perform(post("/api/review-batches/{batchId}/transactions/{transactionId}/decisions",
                        batchId, transactionId).with(user("owner@example.com").roles("USER")).with(csrf())
                        .contentType("application/json")
                        .content("{\"status\":\"RECHECK\",\"reason\":\"증빙 재확인\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECHECK"))
                .andExpect(jsonPath("$.reason").value("증빙 재확인"))
                .andExpect(jsonPath("$.decidedBy").value("owner@example.com"));

        mockMvc.perform(post("/api/review-batches/{batchId}/transactions/{transactionId}/decisions",
                        batchId, transactionId).with(user("owner@example.com").roles("USER")).with(csrf())
                        .contentType("application/json")
                        .content("{\"status\":\"APPROVED\",\"reason\":\"증빙 확인 완료\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from transaction_decision", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "select review_status from settlement_transaction where id = ?", String.class, transactionId))
                .isEqualTo("APPROVED");
    }

    @Test
    void hidesAnotherUsersTransaction() throws Exception {
        mockMvc.perform(post("/api/review-batches/{batchId}/transactions/{transactionId}/decisions",
                        batchId, transactionId).with(user("other@example.com").roles("USER")).with(csrf())
                        .contentType("application/json")
                        .content("{\"status\":\"HOLD\"}"))
                .andExpect(status().isNotFound());
    }
}
