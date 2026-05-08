package com.english.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JwtProvider {

	private final JwtProperties properties;
	private final Clock clock;
	private final SecretKey secretKey;

	@Autowired
	public JwtProvider(JwtProperties properties) {
		this(properties, Clock.systemUTC());
	}

	JwtProvider(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.secretKey = secretKey(properties.getSecret());
	}

	public JwtToken issueToken(Long userId) {
		Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant expiresAt = issuedAt.plus(properties.getExpiration());
		String token = Jwts.builder()
				.subject(String.valueOf(userId))
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.signWith(secretKey)
				.compact();

		return new JwtToken(token, expiresAt);
	}

	public JwtTokenClaims parse(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(secretKey)
					.clock(() -> Date.from(clock.instant()))
					.build()
					.parseSignedClaims(token)
					.getPayload();

			return new JwtTokenClaims(parseUserId(claims.getSubject()), expiration(claims));
		}
		catch (JwtException | IllegalArgumentException exception) {
			throw AuthException.invalidToken();
		}
	}

	public boolean isValid(String token) {
		try {
			parse(token);
			return true;
		}
		catch (AuthException exception) {
			return false;
		}
	}

	private static Long parseUserId(String subject) {
		if (subject == null || subject.isBlank()) {
			throw AuthException.invalidToken();
		}

		try {
			return Long.valueOf(subject);
		}
		catch (NumberFormatException exception) {
			throw AuthException.invalidToken();
		}
	}

	private static Instant expiration(Claims claims) {
		Date expiration = claims.getExpiration();
		if (expiration == null) {
			throw AuthException.invalidToken();
		}
		return expiration.toInstant();
	}

	private static SecretKey secretKey(String secret) {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException("auth.jwt.secret must be at least 32 bytes.");
		}
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
