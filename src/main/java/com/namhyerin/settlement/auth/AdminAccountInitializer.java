package com.namhyerin.settlement.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminAccountInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                                   @Value("${app.admin.email}") String email,
                                   @Value("${app.admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || userRepository.findByEmail(email).isPresent()) {
            return;
        }
        // 환경변수로 전달한 최초 관리자 계정만 자동 생성함
        userRepository.create(email, passwordEncoder.encode(password), "ADMIN");
    }
}
