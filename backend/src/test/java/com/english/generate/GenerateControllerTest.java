package com.english.generate;

import com.english.config.GeminiException;
import com.english.config.GlobalExceptionHandler;
import com.english.config.NoPatternsException;
import com.english.config.NoWordsException;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GenerateControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GenerateService generateService;

    @InjectMocks
    private GenerateController generateController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(generateController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private GenerateResponse sampleResponse() {
        return new GenerateResponse(1L, List.of(
                new GenerateResponse.SentenceResponse(
                        1L, "I want to eat an apple.",
                        "나는 사과를 먹고 싶다.", "ELEMENTARY",
                        List.of("카페에서", "아침에", "마트에서", "친구와", "요리하며"))
        ));
    }

    @Nested
    @DisplayName("POST /api/generate")
    class GenerateEndpoint {

        @Test
        @DisplayName("일반 생성 → 201")
        void generateSuccess() throws Exception {
            given(generateService.generate(any(GenerateRequest.class))).willReturn(sampleResponse());

            mockMvc.perform(post("/api/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"level\":\"ELEMENTARY\",\"count\":10}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sentences[0].englishSentence").value("I want to eat an apple."))
                    .andExpect(jsonPath("$.sentences[0].situations").isArray())
                    .andExpect(jsonPath("$.sentences[0].situations.length()").value(5));
        }

        @Test
        @DisplayName("단어 0개 → 400")
        void generateNoWords() throws Exception {
            given(generateService.generate(any(GenerateRequest.class)))
                    .willThrow(new NoWordsException("등록된 단어가 없습니다"));

            mockMvc.perform(post("/api/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"level\":\"ELEMENTARY\",\"count\":10}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("NO_WORDS"));
        }

        @Test
        @DisplayName("패턴 0개 → 400")
        void generateNoPatterns() throws Exception {
            given(generateService.generate(any(GenerateRequest.class)))
                    .willThrow(new NoPatternsException("등록된 패턴이 없습니다"));

            mockMvc.perform(post("/api/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"level\":\"ELEMENTARY\",\"count\":10}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("NO_PATTERNS"));
        }

        @Test
        @DisplayName("Gemini 장애 → 502")
        void generateGeminiFailure() throws Exception {
            given(generateService.generate(any(GenerateRequest.class)))
                    .willThrow(new GeminiException("Gemini API 호출 실패"));

            mockMvc.perform(post("/api/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"level\":\"ELEMENTARY\",\"count\":10}"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("AI_SERVICE_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /api/generate/word")
    class GenerateByWordEndpoint {

        @Test
        @DisplayName("단어 지정 생성 → 201")
        void generateByWordSuccess() throws Exception {
            given(generateService.generateByWord(eq(1L), any(GenerateRequest.class))).willReturn(sampleResponse());

            mockMvc.perform(post("/api/generate/word")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"level\":\"ELEMENTARY\",\"count\":10,\"wordId\":1}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sentences[0].englishSentence").value("I want to eat an apple."));
        }

        @Test
        @DisplayName("존재하지 않는 단어 → 404")
        void generateByWordNotFound() throws Exception {
            given(generateService.generateByWord(eq(999L), any(GenerateRequest.class)))
                    .willThrow(new NotFoundException("단어를 찾을 수 없습니다"));

            mockMvc.perform(post("/api/generate/word")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"level\":\"ELEMENTARY\",\"count\":10,\"wordId\":999}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/generate/pattern")
    class GenerateByPatternEndpoint {

        @Test
        @DisplayName("패턴 지정 생성 → 201")
        void generateByPatternSuccess() throws Exception {
            given(generateService.generateByPattern(eq(1L), any(GenerateRequest.class))).willReturn(sampleResponse());

            mockMvc.perform(post("/api/generate/pattern")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"level\":\"ELEMENTARY\",\"count\":10,\"patternId\":1}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sentences[0].englishSentence").value("I want to eat an apple."));
        }
    }

    @Nested
    @DisplayName("GET /api/generate/history")
    class HistoryEndpoint {

        @Test
        @DisplayName("이력 조회 → 200 페이지네이션")
        void getHistorySuccess() throws Exception {
            GenerationHistoryResponse historyResponse = new GenerationHistoryResponse(
                    1L, "ELEMENTARY", 10, 10, null, null, LocalDateTime.now());
            given(generateService.getHistory(any()))
                    .willReturn(new PageImpl<>(List.of(historyResponse), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/generate/history")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].level").value("ELEMENTARY"))
                    .andExpect(jsonPath("$.content[0].requestedCount").value(10));
        }
    }
}
