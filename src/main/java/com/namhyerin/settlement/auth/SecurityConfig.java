package com.namhyerin.settlement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/register", "/api/auth/csrf", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                writeJson(response, objectMapper, HttpServletResponse.SC_OK,
                                        Map.of("email", authentication.getName())))
                        .failureHandler((request, response, exception) ->
                                writeJson(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다."))))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                writeJson(response, objectMapper, HttpServletResponse.SC_OK,
                                        Map.of("message", "로그아웃됐습니다."))))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                        writeJson(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                Map.of("message", "로그인이 필요합니다."))))
                .build();
    }

    @Bean
    UserDetailsService userDetailsService(AppUserRepository userRepository) {
        return username -> userRepository.findByEmail(username)
                .map(user -> User.withUsername(user.email())
                        .password(user.passwordHash())
                        .roles(user.role())
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(username));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeJson(HttpServletResponse response, ObjectMapper objectMapper,
                                  int status, Map<String, String> body) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
