package com.english.auth;

import com.english.config.AuthenticationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupRequest request) {
        AuthResponse response = authService.signup(request);

        // 가입 즉시 자동 로그인
        LoginResult loginResult = authService.login(
                new LoginRequest(request.getEmail(), request.getPassword()));

        ResponseCookie cookie = createTokenCookie(loginResult.getToken(), 86400);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = authService.login(request);

        ResponseCookie cookie = createTokenCookie(result.getToken(), 86400);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.getAuthResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = createTokenCookie("", 0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getMe(@CookieValue(name = "token", required = false) String token) {
        if (token == null || !jwtProvider.validateToken(token)) {
            throw new AuthenticationException("인증이 필요합니다");
        }

        String email = jwtProvider.getEmailFromToken(token);
        AuthResponse response = authService.getMe(email);
        return ResponseEntity.ok(response);
    }

    private ResponseCookie createTokenCookie(String token, long maxAge) {
        return ResponseCookie.from("token", token)
                .httpOnly(true)
                .path("/api")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }
}
