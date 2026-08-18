package com.namhyerin.settlement.auth;

import com.namhyerin.settlement.auth.AppUserRepository.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = authService.register(request.email(), request.password());
        return new UserResponse(user.id(), user.email(), user.role());
    }

    @GetMapping("/api/auth/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("email", authentication.getName(), "role",
                authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
    }

    @GetMapping("/api/auth/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        // React가 상태 변경 요청에 사용할 CSRF 토큰을 전달함
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @ExceptionHandler(AppUserRepository.DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> duplicateEmail() {
        return Map.of("message", "이미 가입된 이메일입니다.");
    }

    public record RegisterRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record UserResponse(long id, String email, String role) {
    }
}
