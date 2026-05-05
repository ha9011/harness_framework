package com.english.integration;

import com.english.auth.AuthResponse;
import com.english.auth.LoginRequest;
import com.english.auth.SignupRequest;
import com.english.config.ErrorResponse;
import com.english.word.WordCreateRequest;
import com.english.word.WordEnrichment;
import com.english.word.WordResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("E2E: 회원가입 → 로그인 → me 조회 → 로그아웃 → me 조회 시 401")
    void authE2EFlow() {
        // 1. 회원가입
        SignupRequest signupRequest = new SignupRequest("user@test.com", "password123", "유저");
        ResponseEntity<AuthResponse> signupRes = restTemplate.postForEntity(
                "/api/auth/signup", signupRequest, AuthResponse.class);

        assertThat(signupRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(signupRes.getBody().getEmail()).isEqualTo("user@test.com");
        assertThat(signupRes.getBody().getNickname()).isEqualTo("유저");

        // Set-Cookie 헤더 확인
        List<String> signupCookies = signupRes.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(signupCookies).isNotNull();
        assertThat(signupCookies.stream().anyMatch(c -> c.startsWith("token="))).isTrue();

        // 2. 로그인
        LoginRequest loginRequest = new LoginRequest("user@test.com", "password123");
        ResponseEntity<AuthResponse> loginRes = restTemplate.postForEntity(
                "/api/auth/login", loginRequest, AuthResponse.class);

        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginRes.getBody().getEmail()).isEqualTo("user@test.com");

        // 로그인 Cookie 추출
        String tokenCookie = loginRes.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("token="))
                .findFirst().orElseThrow();
        String tokenValue = tokenCookie.split(";")[0];

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.add(HttpHeaders.COOKIE, tokenValue);

        // 3. me 조회
        ResponseEntity<AuthResponse> meRes = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(authHeaders), AuthResponse.class);

        assertThat(meRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meRes.getBody().getEmail()).isEqualTo("user@test.com");
        assertThat(meRes.getBody().getNickname()).isEqualTo("유저");

        // 4. 로그아웃
        ResponseEntity<Void> logoutRes = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST,
                new HttpEntity<>(authHeaders), Void.class);

        assertThat(logoutRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 로그아웃 후 Set-Cookie에 빈 token (max-age=0)
        String logoutCookie = logoutRes.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("token="))
                .findFirst().orElseThrow();
        assertThat(logoutCookie).contains("Max-Age=0");

        // 5. 로그아웃 후 me 조회 → 401
        ResponseEntity<ErrorResponse> meAfterLogout = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), ErrorResponse.class);

        assertThat(meAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("이메일 중복 가입 시 409")
    void signup_duplicateEmail_returns409() {
        // 첫 번째 가입
        restTemplate.postForEntity("/api/auth/signup",
                new SignupRequest("dup@test.com", "password123", "유저1"),
                AuthResponse.class);

        // 동일 이메일로 두 번째 가입
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/auth/signup",
                new SignupRequest("dup@test.com", "password123", "유저2"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("미인증 GET /api/words → 401")
    void unauthenticated_getWords_returns401() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                "/api/words?page=0&size=10", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("미인증 POST /api/words → 401")
    void unauthenticated_postWords_returns401() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/words", new WordCreateRequest("test", "테스트"), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- 데이터 격리 테스트 ---

    @Test
    @DisplayName("사용자 A 단어 → 사용자 B 미조회")
    void dataIsolation_userA_wordNotVisibleToUserB() {
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/test/", "syn", "tip"));

        // 사용자 A 가입 + 단어 등록
        HttpHeaders headersA = signupAndGetAuthHeaders("a@test.com", "password123", "유저A");
        restTemplate.exchange("/api/words", HttpMethod.POST,
                new HttpEntity<>(new WordCreateRequest("apple", "사과"), headersA),
                WordResponse.class);

        // 사용자 B 가입 + 단어 목록 조회
        HttpHeaders headersB = signupAndGetAuthHeaders("b@test.com", "password123", "유저B");
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/words?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(headersB), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // B의 단어 목록에 A의 "apple"이 없어야 함
        assertThat(response.getBody()).doesNotContain("apple");
    }

    @Test
    @DisplayName("동일 단어 독립 등록 — A와 B 모두 성공 (다른 ID)")
    void dataIsolation_sameWordIndependentRegistration() {
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/test/", "syn", "tip"));

        // 사용자 A 가입 + "apple" 등록
        HttpHeaders headersA = signupAndGetAuthHeaders("a@test.com", "password123", "유저A");
        ResponseEntity<WordResponse> resA = restTemplate.exchange(
                "/api/words", HttpMethod.POST,
                new HttpEntity<>(new WordCreateRequest("apple", "사과"), headersA),
                WordResponse.class);

        assertThat(resA.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 사용자 B 가입 + "apple" 등록
        HttpHeaders headersB = signupAndGetAuthHeaders("b@test.com", "password123", "유저B");
        ResponseEntity<WordResponse> resB = restTemplate.exchange(
                "/api/words", HttpMethod.POST,
                new HttpEntity<>(new WordCreateRequest("apple", "사과"), headersB),
                WordResponse.class);

        assertThat(resB.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 다른 ID
        assertThat(resA.getBody().getId()).isNotEqualTo(resB.getBody().getId());
    }

    @Test
    @DisplayName("사용자 A 삭제 → 사용자 B 영향 없음")
    void dataIsolation_userADelete_doesNotAffectUserB() {
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/test/", "syn", "tip"));

        // 사용자 A 가입 + "apple" 등록
        HttpHeaders headersA = signupAndGetAuthHeaders("a@test.com", "password123", "유저A");
        ResponseEntity<WordResponse> resA = restTemplate.exchange(
                "/api/words", HttpMethod.POST,
                new HttpEntity<>(new WordCreateRequest("apple", "사과"), headersA),
                WordResponse.class);
        Long wordIdA = resA.getBody().getId();

        // 사용자 B 가입 + "apple" 등록
        HttpHeaders headersB = signupAndGetAuthHeaders("b@test.com", "password123", "유저B");
        ResponseEntity<WordResponse> resB = restTemplate.exchange(
                "/api/words", HttpMethod.POST,
                new HttpEntity<>(new WordCreateRequest("apple", "사과"), headersB),
                WordResponse.class);

        // 사용자 A가 자기 단어 삭제
        restTemplate.exchange("/api/words/" + wordIdA, HttpMethod.DELETE,
                new HttpEntity<>(headersA), Void.class);

        // 사용자 B의 단어는 여전히 존재
        ResponseEntity<String> bWords = restTemplate.exchange(
                "/api/words?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(headersB), String.class);

        assertThat(bWords.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bWords.getBody()).contains("apple");
    }
}
