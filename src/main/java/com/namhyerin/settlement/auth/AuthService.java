package com.namhyerin.settlement.auth;

import com.namhyerin.settlement.auth.AppUserRepository.AppUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser register(String email, String password) {
        // 원문 비밀번호를 저장하지 않고 BCrypt 결과만 저장함
        return userRepository.create(email, passwordEncoder.encode(password));
    }
}
