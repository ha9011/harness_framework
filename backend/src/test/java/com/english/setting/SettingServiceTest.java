package com.english.setting;

import com.english.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

    @Mock
    private UserSettingRepository userSettingRepository;

    @InjectMocks
    private SettingService settingService;

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
    }

    @Test
    @DisplayName("설정 조회 - 없으면 기본값(10)으로 자동 생성")
    void getSetting_default() {
        // given
        given(userSettingRepository.findByUser(testUser)).willReturn(Optional.empty());
        UserSetting defaultSetting = new UserSetting(testUser, 10);
        given(userSettingRepository.save(any(UserSetting.class))).willReturn(defaultSetting);

        // when
        UserSettingResponse result = settingService.getSetting(testUser);

        // then
        assertThat(result.getDailyReviewCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("설정 조회 - 기존 설정 반환")
    void getSetting_existing() {
        // given
        UserSetting existing = new UserSetting(testUser, 20);
        given(userSettingRepository.findByUser(testUser)).willReturn(Optional.of(existing));

        // when
        UserSettingResponse result = settingService.getSetting(testUser);

        // then
        assertThat(result.getDailyReviewCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("설정 변경 - dailyReviewCount 업데이트")
    void updateSetting() {
        // given
        UserSetting existing = new UserSetting(testUser, 10);
        given(userSettingRepository.findByUser(testUser)).willReturn(Optional.of(existing));

        // when
        UserSettingResponse result = settingService.updateSetting(testUser, new SettingUpdateRequest(30));

        // then
        assertThat(result.getDailyReviewCount()).isEqualTo(30);
    }
}
