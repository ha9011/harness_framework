package com.english.integration;

import com.english.auth.AuthResponse;
import com.english.auth.SignupRequest;
import com.english.config.GeminiClient;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("english_app_test")
            .withUsername("test")
            .withPassword("test");

    @MockBean
    protected GeminiClient geminiClient;

    @Autowired
    protected TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-32-characters-long");
        registry.add("jwt.expiration", () -> "86400000");
    }

    // 회원가입 후 인증 Cookie가 포함된 HttpHeaders 반환
    protected HttpHeaders signupAndGetAuthHeaders(String email, String password, String nickname) {
        SignupRequest request = new SignupRequest(email, password, nickname);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/signup", request, AuthResponse.class);

        // Set-Cookie 헤더에서 token Cookie 추출
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        String tokenCookie = cookies.stream()
                .filter(c -> c.startsWith("token="))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("인증 Cookie를 찾을 수 없습니다"));

        // token 값만 추출 (token=xxx; Path=/api; ... 에서 token=xxx 부분)
        String tokenValue = tokenCookie.split(";")[0];

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, tokenValue);
        return headers;
    }

    // 기본 테스트 사용자용 편의 메서드
    protected HttpHeaders getDefaultAuthHeaders() {
        return signupAndGetAuthHeaders("test@test.com", "password123", "테스터");
    }
}
