package com.english.review;

import com.english.config.GlobalExceptionHandler;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/reviews/today")
    class GetTodayCards {

        @Test
        @DisplayName("type=WORD → 200 + 카드 배열")
        void getTodayCardsSuccess() throws Exception {
            ReviewCardResponse card = new ReviewCardResponse(
                    1L, "WORD", "RECOGNITION",
                    new ReviewCardResponse.FrontContent("hello", null, null, null, null, null),
                    new ReviewCardResponse.BackContent(null, "안녕", "명사", null, null, null, Collections.emptyList())
            );
            given(reviewService.getTodayCards(eq("WORD"), eq(Collections.emptyList())))
                    .willReturn(List.of(card));

            mockMvc.perform(get("/api/reviews/today")
                            .param("type", "WORD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].itemType").value("WORD"))
                    .andExpect(jsonPath("$[0].front.word").value("hello"))
                    .andExpect(jsonPath("$[0].back.meaning").value("안녕"));
        }

        @Test
        @DisplayName("exclude 파라미터 전달")
        void getTodayCardsWithExclude() throws Exception {
            given(reviewService.getTodayCards(eq("WORD"), eq(List.of(1L, 2L))))
                    .willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/reviews/today")
                            .param("type", "WORD")
                            .param("exclude", "1", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("복습 대상 없음 → 빈 배열")
        void noCardsAvailable() throws Exception {
            given(reviewService.getTodayCards(eq("PATTERN"), eq(Collections.emptyList())))
                    .willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/reviews/today")
                            .param("type", "PATTERN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/reviews/{id}")
    class SubmitResult {

        @Test
        @DisplayName("EASY 결과 제출 → 200")
        void submitEasyResult() throws Exception {
            ReviewResultResponse response = new ReviewResultResponse(
                    1L, "EASY", 3, LocalDate.now().plusDays(3), 2.65, 1);
            given(reviewService.submitResult(eq(1L), eq("EASY"))).willReturn(response);

            mockMvc.perform(post("/api/reviews/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ReviewResultRequest("EASY"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.result").value("EASY"))
                    .andExpect(jsonPath("$.intervalDays").value(3))
                    .andExpect(jsonPath("$.easeFactor").value(2.65))
                    .andExpect(jsonPath("$.reviewCount").value(1));
        }

        @Test
        @DisplayName("존재하지 않는 ID → 404")
        void submitResultNotFound() throws Exception {
            given(reviewService.submitResult(eq(999L), eq("EASY")))
                    .willThrow(new NotFoundException("복습 아이템을 찾을 수 없습니다."));

            mockMvc.perform(post("/api/reviews/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ReviewResultRequest("EASY"))))
                    .andExpect(status().isNotFound());
        }
    }
}
