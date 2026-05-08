package com.english.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

	private String secret = "default-development-jwt-secret-that-is-at-least-32-bytes";
	private Duration expiration = Duration.ofHours(24);

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public Duration getExpiration() {
		return expiration;
	}

	public void setExpiration(Duration expiration) {
		this.expiration = expiration;
	}
}
