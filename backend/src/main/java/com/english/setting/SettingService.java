package com.english.setting;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final UserSettingRepository userSettingRepository;

    @Transactional(readOnly = true)
    public UserSettingResponse getSetting() {
        UserSetting setting = getOrCreateSetting();
        return new UserSettingResponse(setting.getDailyReviewCount());
    }

    @Transactional
    public UserSettingResponse updateSetting(SettingUpdateRequest request) {
        UserSetting setting = getOrCreateSetting();
        setting.updateDailyReviewCount(request.getDailyReviewCount());
        return new UserSettingResponse(setting.getDailyReviewCount());
    }

    /**
     * 단일 레코드 조회 (없으면 기본값으로 생성)
     */
    UserSetting getOrCreateSetting() {
        List<UserSetting> settings = userSettingRepository.findAll();
        if (settings.isEmpty()) {
            return userSettingRepository.save(new UserSetting(10));
        }
        return settings.get(0);
    }
}
