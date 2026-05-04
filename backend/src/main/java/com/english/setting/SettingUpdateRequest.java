package com.english.setting;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettingUpdateRequest {
    @NotNull
    private Integer dailyReviewCount;
}
