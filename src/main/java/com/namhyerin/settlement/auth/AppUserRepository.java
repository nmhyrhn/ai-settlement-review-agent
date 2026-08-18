package com.namhyerin.settlement.auth;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Optional;

@Repository
public class AppUserRepository {

    private final JdbcClient jdbcClient;

    public AppUserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public AppUser create(String email, String passwordHash) {
        String normalizedEmail = normalize(email);
        try {
            jdbcClient.sql("insert into app_user(email, password_hash, role) values (?, ?, 'USER')")
                    .params(normalizedEmail, passwordHash)
                    .update();
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException();
        }
        return findByEmail(normalizedEmail).orElseThrow();
    }

    public Optional<AppUser> findByEmail(String email) {
        return jdbcClient.sql("select id, email, password_hash, role from app_user where email = ?")
                .param(normalize(email))
                .query(AppUser.class)
                .optional();
    }

    // 이메일 대소문자가 다른 중복 계정을 만들지 않도록 정규화함
    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record AppUser(long id, String email, String passwordHash, String role) {
    }

    public static class DuplicateEmailException extends RuntimeException {
    }
}
