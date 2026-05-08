package com.english.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"auth.jwt.secret=auth-controller-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT24H"
})
class AuthControllerTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final AuthService authService;

	@Autowired
	AuthControllerTest(MockMvc mockMvc, AuthService authService) {
		this.mockMvc = mockMvc;
		this.authService = authService;
	}

	@Test
	void signupCreatesUserAndIssuesHttpOnlyJwtCookie() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "signup-controller@example.com",
								  "password": "password123",
								  "nickname": "혜진"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.email").value("signup-controller@example.com"))
				.andExpect(jsonPath("$.nickname").value("혜진"))
				.andReturn();

		assertAuthCookie(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
	}

	@Test
	void loginIssuesHttpOnlyJwtCookie() throws Exception {
		authService.signup("login-controller@example.com", "password123", "혜진");

		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "login-controller@example.com",
								  "password": "password123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.email").value("login-controller@example.com"))
				.andExpect(jsonPath("$.nickname").value("혜진"))
				.andReturn();

		assertAuthCookie(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
	}

	@Test
	void loginFailureReturnsUnauthorizedErrorBody() throws Exception {
		authService.signup("login-failure-controller@example.com", "password123", "혜진");

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "login-failure-controller@example.com",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다"));
	}

	@Test
	void logoutDeletesJwtCookie() throws Exception {
		AuthResult signup = authService.signup("logout-controller@example.com", "password123", "혜진");

		MvcResult result = mockMvc.perform(post("/api/auth/logout")
						.cookie(new Cookie(TOKEN_COOKIE, signup.token())))
				.andExpect(status().isNoContent())
				.andReturn();

		String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookie).contains(TOKEN_COOKIE + "=");
		assertThat(setCookie).contains("Path=/api");
		assertThat(setCookie).contains("Max-Age=0");
		assertThat(setCookie).contains("HttpOnly");
		assertThat(setCookie).contains("SameSite=Lax");
	}

	@Test
	void meReturnsCurrentUserForValidJwtCookie() throws Exception {
		AuthResult signup = authService.signup("me-controller@example.com", "password123", "혜진");

		mockMvc.perform(get("/api/auth/me")
						.cookie(new Cookie(TOKEN_COOKIE, signup.token())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(signup.user().id()))
				.andExpect(jsonPath("$.email").value("me-controller@example.com"))
				.andExpect(jsonPath("$.nickname").value("혜진"));
	}

	@Test
	void meReturnsUnauthorizedErrorWithoutJwtCookie() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	private static void assertAuthCookie(String setCookie) {
		assertThat(setCookie).contains(TOKEN_COOKIE + "=");
		assertThat(setCookie).contains("Path=/api");
		assertThat(setCookie).contains("Max-Age=86400");
		assertThat(setCookie).contains("HttpOnly");
		assertThat(setCookie).contains("SameSite=Lax");
	}
}
