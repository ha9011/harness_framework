package com.english.setting;

import com.english.auth.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public UserSettingResponse getSetting(@AuthenticationPrincipal User user) {
        return settingService.getSetting(user);
    }

    @PutMapping
    public UserSettingResponse updateSetting(@AuthenticationPrincipal User user,
                                             @RequestBody @Valid SettingUpdateRequest request) {
        return settingService.updateSetting(user, request);
    }
}
