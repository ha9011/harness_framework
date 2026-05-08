package com.english.setting;

import com.fasterxml.jackson.databind.JsonNode;

public record UserSettingUpdateRequest(JsonNode value) {

	public String valueAsString() {
		if (value == null || value.isNull()) {
			return null;
		}
		if (value.isTextual() || value.isNumber()) {
			return value.asText();
		}
		return value.toString();
	}
}
