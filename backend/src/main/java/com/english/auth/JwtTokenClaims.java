package com.english.auth;

import java.time.Instant;

public record JwtTokenClaims(
		Long userId,
		Instant expiresAt
) {
}
