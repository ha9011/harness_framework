package com.english.auth;

import com.english.config.AuthenticationException;
import com.english.config.DuplicateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공 - 비밀번호가 BCrypt로 해싱된다")
    void signup_success() {
        // given
        SignupRequest request = new SignupRequest("test@email.com", "password123", "테스터");
        given(userRepository.existsByEmail("test@email.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("$2a$10$hashedPassword");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            // 저장된 User의 password가 해싱된 값인지 검증
            assertThat(user.getPassword()).isEqualTo("$2a$10$hashedPassword");
            return user;
        });

        // when
        AuthResponse response = authService.signup(request);

        // then
        assertThat(response.getEmail()).isEqualTo("test@email.com");
        assertThat(response.getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복 시 DuplicateException")
    void signup_duplicate_email() {
        // given
        SignupRequest request = new SignupRequest("dup@email.com", "password123", "중복");
        given(userRepository.existsByEmail("dup@email.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    @DisplayName("로그인 성공 - JWT 토큰 반환")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "password123");
        User user = new User("test@email.com", "$2a$10$hashedPassword", "테스터");
        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).willReturn(true);
        given(jwtProvider.generateToken("test@email.com")).willReturn("jwt-token");

        // when
        LoginResult result = authService.login(request);

        // then
        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getAuthResponse().getEmail()).isEqualTo("test@email.com");
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 없음 시 AuthenticationException")
    void login_email_not_found() {
        // given
        LoginRequest request = new LoginRequest("notfound@email.com", "password123");
        given(userRepository.findByEmail("notfound@email.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 시 AuthenticationException (동일 메시지)")
    void login_password_mismatch() {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "wrongpassword");
        User user = new User("test@email.com", "$2a$10$hashedPassword", "테스터");
        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongpassword", "$2a$10$hashedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("getMe 성공 - 이메일로 사용자 정보 반환")
    void getMe_success() {
        // given
        User user = new User("test@email.com", "hashed", "테스터");
        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(user));

        // when
        AuthResponse response = authService.getMe("test@email.com");

        // then
        assertThat(response.getEmail()).isEqualTo("test@email.com");
        assertThat(response.getNickname()).isEqualTo("테스터");
    }
}
