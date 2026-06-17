package com.english.auth;

import com.english.config.AuthenticationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    // 쿠키 Secure 여부는 profile 설정값으로 명시 제어 (prod=true, local/default/test=false).
    // Cloudflare Flexible은 CF↔origin이 HTTP라 request.isSecure() 자동판단 불가 → 설정값 분기 (ADR-018)
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/signup")
    public ResponseEntity<AuthTokenResponse> signup(@RequestBody @Valid SignupRequest request) {
        AuthResponse response = authService.signup(request);

        // 가입 즉시 자동 로그인
        LoginResult loginResult = authService.login(
                new LoginRequest(request.getEmail(), request.getPassword()));

        ResponseCookie cookie = createTokenCookie(loginResult.getToken(), 604800);

        // 쿠키 발급은 그대로 유지하고, body에 token을 추가 노출 (PWA localStorage 저장용, ADR-020)
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(AuthTokenResponse.of(response, loginResult.getToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = authService.login(request);

        ResponseCookie cookie = createTokenCookie(result.getToken(), 604800);

        // 쿠키 발급은 그대로 유지하고, body에 token을 추가 노출 (PWA localStorage 저장용, ADR-020)
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(AuthTokenResponse.of(result.getAuthResponse(), result.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = createTokenCookie("", 0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(name = "token", required = false) String cookieToken) {
        // Authorization Bearer 헤더 우선, 없으면 쿠키 (PWA는 쿠키를 폐기하므로 헤더 수용 필수, ADR-020)
        String token = resolveToken(authHeader, cookieToken);
        if (token == null || !jwtProvider.validateToken(token)) {
            throw new AuthenticationException("인증이 필요합니다");
        }

        String email = jwtProvider.getEmailFromToken(token);
        AuthResponse response = authService.getMe(email);
        return ResponseEntity.ok(response);
    }

    private String resolveToken(String authHeader, String cookieToken) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length());
        }
        return cookieToken;
    }

    private ResponseCookie createTokenCookie(String token, long maxAge) {
        return ResponseCookie.from("token", token)
                .httpOnly(true)
                .path("/api")
                .sameSite("Lax")
                .secure(cookieSecure)
                .maxAge(maxAge)
                .build();
    }
}
