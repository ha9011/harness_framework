package com.english.setting;

import com.english.config.ApiErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = UserSettingController.class)
public class UserSettingExceptionHandler {

	@ExceptionHandler(SettingException.class)
	public ResponseEntity<ApiErrorResponse> handleSetting(SettingException exception) {
		HttpStatus status = statusOf(exception.getErrorCode());
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(exception.getErrorCode().name(), exception.getMessage()));
	}

	private static HttpStatus statusOf(SettingErrorCode errorCode) {
		return switch (errorCode) {
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
		};
	}
}
