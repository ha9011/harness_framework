package com.english.auth;

import java.time.Duration;
import org.springframework.http.ResponseCookie;

public final class AuthCookie {

	public static final String NAME = "token";
	private static final String PATH = "/api";
	private static final String SAME_SITE = "Lax";
	private static final Duration MAX_AGE = Duration.ofHours(24);

	private AuthCookie() {
	}

	public static ResponseCookie issue(String token) {
		return ResponseCookie.from(NAME, token)
				.httpOnly(true)
				.path(PATH)
				.sameSite(SAME_SITE)
				.maxAge(MAX_AGE)
				.build();
	}

	public static ResponseCookie delete() {
		return ResponseCookie.from(NAME, "")
				.httpOnly(true)
				.path(PATH)
				.sameSite(SAME_SITE)
				.maxAge(Duration.ZERO)
				.build();
	}
}
