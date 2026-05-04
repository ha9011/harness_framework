package com.english.setting;

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(settingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/settings → 200 + 설정 응답")
    void getSetting() throws Exception {
        given(settingService.getSetting()).willReturn(new UserSettingResponse(10));

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReviewCount").value(10));
    }

    @Test
    @DisplayName("PUT /api/settings → 200 + 변경된 설정")
    void updateSetting() throws Exception {
        given(settingService.updateSetting(any())).willReturn(new UserSettingResponse(30));

        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SettingUpdateRequest(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReviewCount").value(30));
    }
}
