package com.english.setting;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public UserSettingResponse getSetting() {
        return settingService.getSetting();
    }

    @PutMapping
    public UserSettingResponse updateSetting(@RequestBody @Valid SettingUpdateRequest request) {
        return settingService.updateSetting(request);
    }
}
