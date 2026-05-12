package com.english.auth;

import com.english.config.AuthenticationException;
import com.english.config.DuplicateException;
import com.english.config.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/signup 성공 - 201 + Set-Cookie 헤더 존재")
    void signup_success() throws Exception {
        // given
        SignupRequest request = new SignupRequest("test@email.com", "password123", "테스터");
        AuthResponse response = new AuthResponse(1L, "test@email.com", "테스터");
        LoginResult loginResult = new LoginResult("jwt-token", response);
        given(authService.signup(any(SignupRequest.class))).willReturn(response);
        given(authService.login(any(LoginRequest.class))).willReturn(loginResult);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Max-Age=604800")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.nickname").value("테스터"));
    }

    @Test
    @DisplayName("POST /api/auth/signup 실패 - 이메일 중복 409")
    void signup_duplicate_email() throws Exception {
        // given
        SignupRequest request = new SignupRequest("dup@email.com", "password123", "중복");
        given(authService.signup(any(SignupRequest.class)))
                .willThrow(new DuplicateException("이미 사용 중인 이메일입니다"));

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    @DisplayName("POST /api/auth/signup 실패 - 비밀번호 8글자 미만 400")
    void signup_short_password() throws Exception {
        // given
        SignupRequest request = new SignupRequest("test@email.com", "short", "테스터");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login 성공 - 200 + Set-Cookie 헤더 존재")
    void login_success() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "password123");
        AuthResponse response = new AuthResponse(1L, "test@email.com", "테스터");
        LoginResult loginResult = new LoginResult("jwt-token", response);
        given(authService.login(any(LoginRequest.class))).willReturn(loginResult);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Max-Age=604800")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@email.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login 실패 - 401")
    void login_failure() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "wrongpassword");
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다"));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /api/auth/logout - 204 + Max-Age=0 Cookie")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("GET /api/auth/me 성공 - 200")
    void getMe_success() throws Exception {
        // given
        AuthResponse response = new AuthResponse(1L, "test@email.com", "테스터");
        given(jwtProvider.validateToken("valid-token")).willReturn(true);
        given(jwtProvider.getEmailFromToken("valid-token")).willReturn("test@email.com");
        given(authService.getMe("test@email.com")).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("token", "valid-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.nickname").value("테스터"));
    }
}
