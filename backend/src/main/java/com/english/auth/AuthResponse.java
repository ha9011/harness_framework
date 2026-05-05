package com.english.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String email;
    private String nickname;

    public static AuthResponse from(User user) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
