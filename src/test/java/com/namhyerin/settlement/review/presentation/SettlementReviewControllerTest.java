package com.namhyerin.settlement.review.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "openai.api-key=",
        "spring.datasource.url=jdbc:h2:mem:controller-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class SettlementReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadsCsvAndReturnsReviewResults() throws Exception {
        var csv = new MockMultipartFile("file", "settlements.csv", "text/csv", ("""
                transactionId,transactionDate,merchant,amount,receiptNumber
                T-001,2026-08-18,ABC상사,1250000,
                """).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/reviews/upload").file(csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transaction.transactionId").value("T-001"))
                .andExpect(jsonPath("$[0].violations.length()").value(2))
                .andExpect(jsonPath("$[0].explanation.available").value(false));
    }

    @Test
    void storesAndReadsUserDecision() throws Exception {
        mockMvc.perform(post("/api/reviews/T-009/decisions")
                        .contentType("application/json")
                        .content("{\"status\":\"RECHECK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECHECK"));

        mockMvc.perform(get("/api/reviews/T-009/decisions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("T-009"));
    }
}
