package com.english.auth;

import java.time.Instant;

public record AuthResult(
		CurrentUserResponse user,
		String token,
		Instant expiresAt
) {

	public static AuthResult from(User user, JwtToken token) {
		return new AuthResult(CurrentUserResponse.from(user), token.value(), token.expiresAt());
	}
}
