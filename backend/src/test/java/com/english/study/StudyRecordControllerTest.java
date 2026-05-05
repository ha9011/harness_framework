package com.english.study;

import com.english.auth.User;
import com.english.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudyRecordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StudyRecordService studyRecordService;

    @InjectMocks
    private StudyRecordController studyRecordController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(studyRecordController)
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
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/study-records → 200 + 페이지네이션 응답")
    void getRecords() throws Exception {
        StudyRecordResponse r1 = new StudyRecordResponse(1L, 2, "2026-05-05", 3, 1);
        Page<StudyRecordResponse> page = new PageImpl<>(List.of(r1), PageRequest.of(0, 10), 1);
        given(studyRecordService.getRecords(eq(testUser), any())).willReturn(page);

        mockMvc.perform(get("/api/study-records?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].dayNumber").value(2))
                .andExpect(jsonPath("$.content[0].wordCount").value(3))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
