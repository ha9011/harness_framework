package com.english.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = {
		"auth.jwt.secret=auth-service-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT24H"
})
class AuthServiceTest {

	private static final String AUTH_FAILURE_MESSAGE = "이메일 또는 비밀번호가 올바르지 않습니다";

	private final AuthService authService;
	private final JwtProvider jwtProvider;
	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;

	@Autowired
	AuthServiceTest(
			AuthService authService,
			JwtProvider jwtProvider,
			PasswordEncoder passwordEncoder,
			UserRepository userRepository) {
		this.authService = authService;
		this.jwtProvider = jwtProvider;
		this.passwordEncoder = passwordEncoder;
		this.userRepository = userRepository;
	}

	@Test
	void signupStoresBCryptHashAndReturnsUserAndToken() {
		AuthResult result = authService.signup("signup-service@example.com", "password123", "혜진");

		assertThat(result.user().id()).isNotNull();
		assertThat(result.user().email()).isEqualTo("signup-service@example.com");
		assertThat(result.user().nickname()).isEqualTo("혜진");
		assertThat(result.token()).isNotBlank();
		assertThat(jwtProvider.parse(result.token()).userId()).isEqualTo(result.user().id());

		User saved = userRepository.findByEmail("signup-service@example.com").orElseThrow();
		assertThat(saved.getPassword()).isNotEqualTo("password123");
		assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
	}

	@Test
	void signupRejectsShortPassword() {
		Throwable thrown = catchThrowable(() -> authService.signup("short-password@example.com", "short", "혜진"));

		assertThat(thrown)
				.isInstanceOf(AuthException.class)
				.hasMessage("비밀번호는 8자 이상이어야 합니다");
		assertThat(((AuthException) thrown).getErrorCode()).isEqualTo(AuthErrorCode.BAD_REQUEST);
	}

	@Test
	void signupRejectsDuplicateEmailWithDuplicateError() {
		authService.signup("duplicate-service@example.com", "password123", "혜진");

		Throwable thrown = catchThrowable(() -> authService.signup("duplicate-service@example.com", "password456", "민수"));

		assertThat(thrown)
				.isInstanceOf(AuthException.class)
				.hasMessage("이미 등록된 이메일입니다");
		assertThat(((AuthException) thrown).getErrorCode()).isEqualTo(AuthErrorCode.DUPLICATE);
	}

	@Test
	void loginReturnsSameUserAndTokenForValidCredentials() {
		AuthResult signup = authService.signup("login-service@example.com", "password123", "혜진");

		AuthResult login = authService.login("login-service@example.com", "password123");

		assertThat(login.user()).isEqualTo(signup.user());
		assertThat(jwtProvider.parse(login.token()).userId()).isEqualTo(signup.user().id());
	}

	@Test
	void loginFailureDoesNotRevealWhetherEmailOrPasswordWasWrong() {
		authService.signup("login-failure@example.com", "password123", "혜진");

		Throwable missingEmail = catchThrowable(() -> authService.login("missing-login@example.com", "password123"));
		Throwable wrongPassword = catchThrowable(() -> authService.login("login-failure@example.com", "wrong-password"));

		assertThat(missingEmail)
				.isInstanceOf(AuthException.class)
				.hasMessage(AUTH_FAILURE_MESSAGE);
		assertThat(wrongPassword)
				.isInstanceOf(AuthException.class)
				.hasMessage(AUTH_FAILURE_MESSAGE);
		assertThat(((AuthException) missingEmail).getErrorCode()).isEqualTo(AuthErrorCode.UNAUTHORIZED);
		assertThat(((AuthException) wrongPassword).getErrorCode()).isEqualTo(AuthErrorCode.UNAUTHORIZED);
	}

	@Test
	void currentUserReturnsUserResponseById() {
		AuthResult signup = authService.signup("current-user@example.com", "password123", "혜진");

		CurrentUserResponse currentUser = authService.currentUser(signup.user().id());

		assertThat(currentUser).isEqualTo(signup.user());
	}
}
