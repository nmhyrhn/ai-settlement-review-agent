package com.namhyerin.settlement.policy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "openai.api-key=",
        "spring.datasource.url=jdbc:h2:mem:policy-controller-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class PolicyDocumentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PolicyDocumentService service;

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@example.com")
    void adminRegistersPolicyDocument() throws Exception {
        when(service.register(anyString(), any(), anyString())).thenReturn(
                new PolicyDocumentRepository.PolicySummary(1, "정산 규정", 1, "policy.md",
                        "ACTIVE", "admin@example.com", LocalDateTime.now()));
        var file = new MockMultipartFile("file", "policy.md", "text/markdown", "규정 내용".getBytes());

        mockMvc.perform(multipart("/api/admin/policies")
                        .file(file)
                        .param("title", "정산 규정")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotRegisterPolicyDocument() throws Exception {
        var file = new MockMultipartFile("file", "policy.md", "text/markdown", "규정 내용".getBytes());

        mockMvc.perform(multipart("/api/admin/policies")
                        .file(file)
                        .param("title", "정산 규정")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
