package com.english.setting;

import com.english.auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final UserSettingRepository userSettingRepository;

    @Transactional
    public UserSettingResponse getSetting(User user) {
        UserSetting setting = getOrCreateSetting(user);
        return new UserSettingResponse(setting.getDailyReviewCount());
    }

    @Transactional
    public UserSettingResponse updateSetting(User user, SettingUpdateRequest request) {
        UserSetting setting = getOrCreateSetting(user);
        setting.updateDailyReviewCount(request.getDailyReviewCount());
        return new UserSettingResponse(setting.getDailyReviewCount());
    }

    /**
     * 사용자별 설정 조회 (없으면 기본값으로 생성)
     */
    UserSetting getOrCreateSetting(User user) {
        return userSettingRepository.findByUser(user)
                .orElseGet(() -> userSettingRepository.save(new UserSetting(user, 10)));
    }
}
