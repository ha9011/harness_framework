package com.english.dashboard;

import com.english.auth.User;
import com.english.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
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
                })
                .build();
    }

    @Test
    @DisplayName("GET /api/dashboard → 200 + 대시보드 응답")
    void getDashboard() throws Exception {
        DashboardResponse response = new DashboardResponse(
                10, 5, 20, 3,
                new DashboardResponse.ReviewRemaining(5, 3, 8),
                List.of(new DashboardResponse.StudyRecordDto(1L, 3, "2026-05-05", 3, 1))
        );
        given(dashboardService.getDashboard(testUser)).willReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wordCount").value(10))
                .andExpect(jsonPath("$.patternCount").value(5))
                .andExpect(jsonPath("$.sentenceCount").value(20))
                .andExpect(jsonPath("$.streak").value(3))
                .andExpect(jsonPath("$.todayReviewRemaining.word").value(5))
                .andExpect(jsonPath("$.todayReviewRemaining.pattern").value(3))
                .andExpect(jsonPath("$.todayReviewRemaining.sentence").value(8))
                .andExpect(jsonPath("$.recentStudyRecords").isArray())
                .andExpect(jsonPath("$.recentStudyRecords[0].dayNumber").value(3));
    }

    @Test
    @DisplayName("GET /api/dashboard → 첫 사용자 (모든 카운트 0)")
    void getDashboard_empty() throws Exception {
        DashboardResponse response = new DashboardResponse(
                0, 0, 0, 0,
                new DashboardResponse.ReviewRemaining(0, 0, 0),
                Collections.emptyList()
        );
        given(dashboardService.getDashboard(testUser)).willReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wordCount").value(0))
                .andExpect(jsonPath("$.streak").value(0))
                .andExpect(jsonPath("$.recentStudyRecords").isEmpty());
    }
}
