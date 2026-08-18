package com.namhyerin.settlement.batch;

import com.namhyerin.settlement.auth.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.namhyerin.settlement.policy.PolicyRagClient;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "openai.api-key=",
        "spring.datasource.url=jdbc:h2:mem:batch-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class ReviewBatchControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;
    @MockitoBean PolicyRagClient ragClient;

    @BeforeEach
    void createUser() {
        when(ragClient.explain(any(), any())).thenReturn(new PolicyRagClient.ExplanationResponse(
                "정책 근거 설명", List.of(), "OPENAI_RAG"));
        if (userRepository.findByEmail("user@example.com").isEmpty()) {
            userRepository.create("user@example.com", passwordEncoder.encode("password123"));
        }
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void createsBatchAndPersistsTransactions() throws Exception {
        var csv = new MockMultipartFile("file", "settlements.csv", "text/csv", """
                transactionId,transactionDate,merchant,amount,receiptNumber
                B-001,2026-08-18,ABC상사,1250000,
                B-002,2026-08-19,XYZ상사,50000,R-002
                """.getBytes());

        mockMvc.perform(multipart("/api/review-batches").file(csv).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.violationCount").value(2))
                .andExpect(jsonPath("$.explanationCount").value(1))
                .andExpect(jsonPath("$.totalCount").value(2));

        Integer count = jdbcTemplate.queryForObject("select count(*) from settlement_transaction", Integer.class);
        assertThat(count).isEqualTo(2);
        Integer violations = jdbcTemplate.queryForObject("select count(*) from review_violation", Integer.class);
        assertThat(violations).isEqualTo(2);
        Integer explanations = jdbcTemplate.queryForObject("select count(*) from review_ai_explanation", Integer.class);
        assertThat(explanations).isEqualTo(1);
    }
}
