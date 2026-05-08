package com.english.auth;

import java.time.Instant;

public record JwtToken(
		String value,
		Instant expiresAt
) {
}
