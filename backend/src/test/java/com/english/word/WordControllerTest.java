package com.english.word;

import com.english.auth.User;
import com.english.config.DuplicateException;
import com.english.config.EmptyRequestException;
import com.english.config.GlobalExceptionHandler;
import com.english.config.NotFoundException;
import com.english.pattern.PatternService;
import com.english.pattern.WordExtractResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WordService wordService;

    @Mock
    private PatternService patternService;

    @InjectMocks
    private WordController wordController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(wordController)
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
    @DisplayName("POST /api/words")
    class CreateWord {

        @Test
        @DisplayName("단건 등록 성공 → 201")
        void createSuccess() throws Exception {
            WordResponse response = new WordResponse(1L, "apple", "사과", "명사", "ˈæpəl",
                    "fruit", "tip", false, LocalDateTime.now());
            given(wordService.create(eq(testUser), any(WordCreateRequest.class))).willReturn(response);

            mockMvc.perform(post("/api/words")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"word\":\"apple\",\"meaning\":\"사과\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.word").value("apple"))
                    .andExpect(jsonPath("$.meaning").value("사과"))
                    .andExpect(jsonPath("$.partOfSpeech").value("명사"));
        }

        @Test
        @DisplayName("중복 등록 → 409")
        void createDuplicate() throws Exception {
            given(wordService.create(eq(testUser), any(WordCreateRequest.class)))
                    .willThrow(new DuplicateException("이미 등록된 단어입니다"));

            mockMvc.perform(post("/api/words")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"word\":\"apple\",\"meaning\":\"사과\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE"));
        }
    }

    @Nested
    @DisplayName("POST /api/words/bulk")
    class BulkCreate {

        @Test
        @DisplayName("벌크 등록 성공 → 201")
        void bulkCreateSuccess() throws Exception {
            BulkCreateResponse response = new BulkCreateResponse(2, 1, 0, List.of());
            given(wordService.bulkCreate(eq(testUser), anyList())).willReturn(response);

            mockMvc.perform(post("/api/words/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[{\"word\":\"apple\",\"meaning\":\"사과\"},{\"word\":\"banana\",\"meaning\":\"바나나\"}]"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.saved").value(2))
                    .andExpect(jsonPath("$.skipped").value(1));
        }

        @Test
        @DisplayName("빈 배열 → 400")
        void bulkCreateEmpty() throws Exception {
            given(wordService.bulkCreate(eq(testUser), anyList()))
                    .willThrow(new EmptyRequestException("등록할 단어가 없습니다"));

            mockMvc.perform(post("/api/words/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("EMPTY_REQUEST"));
        }
    }

    @Nested
    @DisplayName("GET /api/words")
    class GetList {

        @Test
        @DisplayName("목록 조회 → 200")
        void getListSuccess() throws Exception {
            WordListResponse item = new WordListResponse(1L, "apple", "사과", "명사", false, LocalDateTime.now());
            given(wordService.getList(eq(testUser), any(), any(), anyBoolean(), any(), any()))
                    .willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/words")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].word").value("apple"));
        }
    }

    @Nested
    @DisplayName("GET /api/words/{id}")
    class GetDetail {

        @Test
        @DisplayName("상세 조회 → 200")
        void getDetailSuccess() throws Exception {
            WordDetailResponse response = new WordDetailResponse(1L, "apple", "사과", "명사",
                    "ˈæpəl", "fruit", "tip", false, LocalDateTime.now(), List.of());
            given(wordService.getDetail(testUser, 1L)).willReturn(response);

            mockMvc.perform(get("/api/words/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.word").value("apple"))
                    .andExpect(jsonPath("$.examples").isArray());
        }

        @Test
        @DisplayName("존재하지 않는 ID → 404")
        void getDetailNotFound() throws Exception {
            given(wordService.getDetail(testUser, 999L))
                    .willThrow(new NotFoundException("단어를 찾을 수 없습니다"));

            mockMvc.perform(get("/api/words/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/words/{id}/important")
    class ToggleImportant {

        @Test
        @DisplayName("중요 토글 → 200")
        void toggleSuccess() throws Exception {
            WordResponse response = new WordResponse(1L, "apple", "사과", "명사", "ˈæpəl",
                    "fruit", "tip", true, LocalDateTime.now());
            given(wordService.toggleImportant(testUser, 1L)).willReturn(response);

            mockMvc.perform(patch("/api/words/1/important"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.important").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /api/words/{id}")
    class DeleteWord {

        @Test
        @DisplayName("삭제 → 204")
        void deleteSuccess() throws Exception {
            doNothing().when(wordService).delete(testUser, 1L);

            mockMvc.perform(delete("/api/words/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("존재하지 않는 ID → 404")
        void deleteNotFound() throws Exception {
            doThrow(new NotFoundException("단어를 찾을 수 없습니다"))
                    .when(wordService).delete(testUser, 999L);

            mockMvc.perform(delete("/api/words/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/words/extract")
    class ExtractWords {

        @Test
        @DisplayName("단어 이미지 추출 성공 → 200")
        void extractSuccess() throws Exception {
            WordExtractResponse response = new WordExtractResponse(
                    List.of(new WordExtractResponse.ExtractedWord("apple", "사과"))
            );
            given(patternService.extractWordsFromImage(any())).willReturn(response);

            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.png", "image/png", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/words/extract").file(image))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.words[0].word").value("apple"));
        }
    }
}
