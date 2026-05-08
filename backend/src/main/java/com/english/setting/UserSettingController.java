package com.english.setting;

import com.english.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class UserSettingController {

	private final UserSettingService userSettingService;

	public UserSettingController(UserSettingService userSettingService) {
		this.userSettingService = userSettingService;
	}

	@GetMapping
	public UserSettingResponse getSettings(@AuthenticationPrincipal AuthenticatedUser principal) {
		return userSettingService.getSettings(principal.userId());
	}

	@PutMapping("/{key}")
	public UserSettingResponse updateSetting(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable String key,
			@RequestBody UserSettingUpdateRequest request
	) {
		return userSettingService.updateSetting(principal.userId(), key, request.valueAsString());
	}
}
