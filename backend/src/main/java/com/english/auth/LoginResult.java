package com.english.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResult {
    private String token;
    private AuthResponse authResponse;
}
