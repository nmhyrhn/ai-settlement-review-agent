package com.namhyerin.settlement.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "openai.api-key=",
        "spring.datasource.url=jdbc:h2:mem:auth-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void registersUserWithNormalizedEmailAndEncodedPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"USER@Example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        var saved = userRepository.findByEmail("user@example.com").orElseThrow();
        assertThat(saved.passwordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.passwordHash())).isTrue();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        userRepository.create("duplicate@example.com", passwordEncoder.encode("password123"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"DUPLICATE@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void protectsReviewApiFromAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/reviews/T-001/decisions"))
                .andExpect(status().isUnauthorized());
    }
}
