package com.english.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.auth.AuthResult;
import com.english.auth.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"auth.jwt.secret=security-config-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT24H"
})
class SecurityConfigTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final AuthService authService;

	@Autowired
	SecurityConfigTest(MockMvc mockMvc, AuthService authService) {
		this.mockMvc = mockMvc;
		this.authService = authService;
	}

	@Test
	void publicEndpointsDoNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void apiEndpointsExceptPublicAuthAndHealthRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/words"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").isNotEmpty());

		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	void validJwtCookieAuthenticatesProtectedApiBoundary() throws Exception {
		AuthResult signup = authService.signup("security-boundary@example.com", "password123", "혜진");

		mockMvc.perform(get("/api/words")
						.cookie(new Cookie(TOKEN_COOKIE, signup.token())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void invalidJwtCookieReturnsUnauthorizedErrorForProtectedApi() throws Exception {
		mockMvc.perform(get("/api/words")
						.cookie(new Cookie(TOKEN_COOKIE, "not-a-jwt")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").isNotEmpty());
	}
}
