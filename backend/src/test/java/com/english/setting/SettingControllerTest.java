package com.english.setting;

import com.english.auth.User;
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
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SettingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SettingService settingService;

    @InjectMocks
    private SettingController settingController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(settingController)
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
    @DisplayName("GET /api/settings → 200 + 설정 응답")
    void getSetting() throws Exception {
        given(settingService.getSetting(testUser)).willReturn(new UserSettingResponse(10));

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReviewCount").value(10));
    }

    @Test
    @DisplayName("PUT /api/settings → 200 + 변경된 설정")
    void updateSetting() throws Exception {
        given(settingService.updateSetting(eq(testUser), any())).willReturn(new UserSettingResponse(30));

        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SettingUpdateRequest(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReviewCount").value(30));
    }
}
