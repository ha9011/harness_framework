package com.english.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인/회원가입 전용 응답 DTO.
 * AuthResponse(/auth/me 공용 + 다수 테스트의 3-arg 생성자 사용)를 깨지 않기 위해
 * token을 노출하는 별도 DTO를 둔다 (ADR-020 하이브리드 인증, PWA localStorage 저장용).
 */
@Getter
@AllArgsConstructor
public class AuthTokenResponse {
    private Long id;
    private String email;
    private String nickname;
    private String token;

    public static AuthTokenResponse of(AuthResponse authResponse, String token) {
        return new AuthTokenResponse(
                authResponse.getId(),
                authResponse.getEmail(),
                authResponse.getNickname(),
                token);
    }
}
