package com.english.setting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

    @Mock
    private UserSettingRepository userSettingRepository;

    @InjectMocks
    private SettingService settingService;

    @Test
    @DisplayName("설정 조회 - 없으면 기본값(10)으로 자동 생성")
    void getSetting_default() {
        // given
        given(userSettingRepository.findAll()).willReturn(Collections.emptyList());
        UserSetting defaultSetting = new UserSetting(10);
        given(userSettingRepository.save(any(UserSetting.class))).willReturn(defaultSetting);

        // when
        UserSettingResponse result = settingService.getSetting();

        // then
        assertThat(result.getDailyReviewCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("설정 조회 - 기존 설정 반환")
    void getSetting_existing() {
        // given
        UserSetting existing = new UserSetting(20);
        given(userSettingRepository.findAll()).willReturn(List.of(existing));

        // when
        UserSettingResponse result = settingService.getSetting();

        // then
        assertThat(result.getDailyReviewCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("설정 변경 - dailyReviewCount 업데이트")
    void updateSetting() {
        // given
        UserSetting existing = new UserSetting(10);
        given(userSettingRepository.findAll()).willReturn(List.of(existing));

        // when
        UserSettingResponse result = settingService.updateSetting(new SettingUpdateRequest(30));

        // then
        assertThat(result.getDailyReviewCount()).isEqualTo(30);
    }
}
