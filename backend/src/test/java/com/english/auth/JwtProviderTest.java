package com.english.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;
    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";
    private static final long EXPIRATION = 86400000L; // 24시간

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, EXPIRATION);
    }

    @Test
    @DisplayName("토큰 생성 후 이메일 추출 성공")
    void generateToken_thenExtractEmail() {
        // given
        String email = "test@example.com";

        // when
        String token = jwtProvider.generateToken(email);
        String extracted = jwtProvider.getEmailFromToken(token);

        // then
        assertThat(extracted).isEqualTo(email);
    }

    @Test
    @DisplayName("토큰 생성 후 유효성 검증 성공")
    void generateToken_thenValidate() {
        // given
        String token = jwtProvider.generateToken("test@example.com");

        // when & then
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰 유효성 검증 실패")
    void expiredToken_validateFails() {
        // given - 만료 시간을 음수로 설정
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1000L);
        String token = expiredProvider.generateToken("test@example.com");

        // when & then
        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("변조된 토큰 유효성 검증 실패")
    void tamperedToken_validateFails() {
        // given
        String token = jwtProvider.generateToken("test@example.com");
        String tampered = token + "tampered";

        // when & then
        assertThat(jwtProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("null/빈 문자열 토큰 유효성 검증 실패")
    void nullOrEmptyToken_validateFails() {
        assertThat(jwtProvider.validateToken(null)).isFalse();
        assertThat(jwtProvider.validateToken("")).isFalse();
        assertThat(jwtProvider.validateToken("   ")).isFalse();
    }
}
