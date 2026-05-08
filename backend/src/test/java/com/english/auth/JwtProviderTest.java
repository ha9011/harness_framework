package com.english.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@SpringBootTest(classes = JwtProvider.class)
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
		"auth.jwt.secret=jwt-provider-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT3H"
})
class JwtProviderTest {

	private static final String SECRET = "jwt-provider-unit-secret-that-is-at-least-32-bytes";

	private final JwtProvider jwtProvider;

	@Autowired
	JwtProviderTest(JwtProvider jwtProvider) {
		this.jwtProvider = jwtProvider;
	}

	@Test
	void issuesTokenWithUserIdSubjectAndConfiguredExpiration() {
		Instant beforeExpectedExpiration = Instant.now().plus(Duration.ofHours(3)).minusSeconds(1);

		JwtToken token = jwtProvider.issueToken(42L);

		Instant afterExpectedExpiration = Instant.now().plus(Duration.ofHours(3)).plusSeconds(1);
		JwtTokenClaims claims = jwtProvider.parse(token.value());
		assertThat(claims.userId()).isEqualTo(42L);
		assertThat(claims.expiresAt()).isBetween(beforeExpectedExpiration, afterExpectedExpiration);
		assertThat(token.expiresAt()).isEqualTo(claims.expiresAt());
	}

	@Test
	void rejectsExpiredToken() {
		Instant issuedAt = Instant.parse("2026-05-08T00:00:00Z");
		JwtProvider issuer = jwtProviderAt(issuedAt, Duration.ofHours(24));
		String token = issuer.issueToken(7L).value();

		JwtProvider expiredParser = jwtProviderAt(issuedAt.plus(Duration.ofHours(24)).plusMillis(1), Duration.ofHours(24));

		assertThatThrownBy(() -> expiredParser.parse(token))
				.isInstanceOf(AuthException.class)
				.hasMessage("인증 토큰이 올바르지 않습니다");
	}

	@Test
	void rejectsInvalidToken() {
		assertThatThrownBy(() -> jwtProvider.parse("not-a-jwt"))
				.isInstanceOf(AuthException.class)
				.hasMessage("인증 토큰이 올바르지 않습니다");
	}

	@Test
	void validatesTokenByParsingClaims() {
		String validToken = jwtProvider.issueToken(99L).value();

		assertThat(jwtProvider.isValid(validToken)).isTrue();
		assertThat(jwtProvider.isValid("not-a-jwt")).isFalse();
	}

	private static JwtProvider jwtProviderAt(Instant instant, Duration expiration) {
		JwtProperties properties = new JwtProperties();
		properties.setSecret(SECRET);
		properties.setExpiration(expiration);
		return new JwtProvider(properties, Clock.fixed(instant, ZoneOffset.UTC));
	}
}
