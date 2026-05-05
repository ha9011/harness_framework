package com.english.pattern;

import com.english.auth.User;
import com.english.config.DuplicateException;
import com.english.config.GeminiException;
import com.english.config.GlobalExceptionHandler;
import com.english.config.InvalidImageException;
import com.english.config.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PatternControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatternService patternService;

    @InjectMocks
    private PatternController patternController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("test@test.com", "password", "테스터");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mockMvc = MockMvcBuilders.standaloneSetup(patternController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(User.class);
                    }
                    @Override
                    public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                            org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                            org.springframework.web.context.request.NativeWebRequest webRequest,
                            org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return testUser;
                    }
                }, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("POST /api/patterns")
    class CreatePattern {

        @Test
        @DisplayName("등록 성공 → 201")
        void createSuccess() throws Exception {
            PatternResponse response = new PatternResponse(1L, "I want to ~", "~하고 싶다", LocalDateTime.now());
            given(patternService.create(eq(testUser), any(PatternCreateRequest.class))).willReturn(response);

            mockMvc.perform(post("/api/patterns")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"template\":\"I want to ~\",\"description\":\"~하고 싶다\",\"examples\":[]}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.template").value("I want to ~"))
                    .andExpect(jsonPath("$.description").value("~하고 싶다"));
        }

        @Test
        @DisplayName("중복 패턴 → 409")
        void createDuplicate() throws Exception {
            given(patternService.create(eq(testUser), any(PatternCreateRequest.class)))
                    .willThrow(new DuplicateException("이미 등록된 패턴입니다"));

            mockMvc.perform(post("/api/patterns")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"template\":\"I want to ~\",\"description\":\"~하고 싶다\",\"examples\":[]}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE"));
        }
    }

    @Nested
    @DisplayName("GET /api/patterns")
    class GetList {

        @Test
        @DisplayName("목록 조회 → 200")
        void getListSuccess() throws Exception {
            PatternListResponse item = new PatternListResponse(1L, "I want to ~", "~하고 싶다", 5, LocalDateTime.now());
            given(patternService.getList(eq(testUser), any())).willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/patterns")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].template").value("I want to ~"));
        }
    }

    @Nested
    @DisplayName("GET /api/patterns/{id}")
    class GetDetail {

        @Test
        @DisplayName("상세 조회 → 200")
        void getDetailSuccess() throws Exception {
            PatternDetailResponse response = new PatternDetailResponse(
                    1L, "I want to ~", "~하고 싶다", LocalDateTime.now(),
                    List.of(new PatternDetailResponse.ExampleResponse("I want to go.", "가고 싶다.", 0))
            );
            given(patternService.getDetail(testUser, 1L)).willReturn(response);

            mockMvc.perform(get("/api/patterns/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.template").value("I want to ~"))
                    .andExpect(jsonPath("$.examples[0].sentence").value("I want to go."));
        }

        @Test
        @DisplayName("존재하지 않는 ID → 404")
        void getDetailNotFound() throws Exception {
            given(patternService.getDetail(testUser, 999L))
                    .willThrow(new NotFoundException("패턴을 찾을 수 없습니다"));

            mockMvc.perform(get("/api/patterns/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/patterns/{id}")
    class DeletePattern {

        @Test
        @DisplayName("삭제 → 204")
        void deleteSuccess() throws Exception {
            doNothing().when(patternService).delete(testUser, 1L);

            mockMvc.perform(delete("/api/patterns/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("존재하지 않는 ID → 404")
        void deleteNotFound() throws Exception {
            doThrow(new NotFoundException("패턴을 찾을 수 없습니다"))
                    .when(patternService).delete(testUser, 999L);

            mockMvc.perform(delete("/api/patterns/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/patterns/extract")
    class ExtractPattern {

        @Test
        @DisplayName("이미지 추출 성공 → 200")
        void extractSuccess() throws Exception {
            PatternExtractResponse response = new PatternExtractResponse(
                    "I want to ~", "~하고 싶다",
                    List.of(new PatternExtractResponse.ExtractedExample("I want to go.", "가고 싶다."))
            );
            given(patternService.extractFromImage(any())).willReturn(response);

            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.png", "image/png", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/patterns/extract").file(image))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.template").value("I want to ~"));
        }

        @Test
        @DisplayName("이미지 형식 오류 → 400")
        void extractInvalidFormat() throws Exception {
            given(patternService.extractFromImage(any()))
                    .willThrow(new InvalidImageException("지원하지 않는 이미지 형식입니다"));

            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.txt", "text/plain", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/patterns/extract").file(image))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_IMAGE_FORMAT"));
        }

        @Test
        @DisplayName("Gemini 장애 → 502")
        void extractGeminiFailure() throws Exception {
            given(patternService.extractFromImage(any()))
                    .willThrow(new GeminiException("Gemini API 호출 실패"));

            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.png", "image/png", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/patterns/extract").file(image))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("AI_SERVICE_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /api/words/extract")
    class ExtractWords {

        @Test
        @DisplayName("단어 이미지 추출 성공 → 200")
        void extractSuccess() throws Exception {
            // WordController에서 처리 — 이 테스트는 WordControllerTest에서 검증
        }
    }
}
