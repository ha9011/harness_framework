package com.english.config;

import com.english.auth.AuthErrorCode;
import com.english.auth.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		return ResponseEntity.badRequest()
				.body(new ApiErrorResponse("validation_error", "Invalid request."));
	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ApiErrorResponse> handleAuth(AuthException exception) {
		HttpStatus status = statusOf(exception.getErrorCode());
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(exception.getErrorCode().name(), exception.getMessage()));
	}

	@ExceptionHandler(ErrorResponseException.class)
	public ResponseEntity<ApiErrorResponse> handleErrorResponse(ErrorResponseException exception) {
		HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(status.name().toLowerCase(), exception.getMessage()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse("NOT_FOUND", "요청한 API를 찾을 수 없습니다"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("internal_server_error", "Unexpected server error."));
	}

	private static HttpStatus statusOf(AuthErrorCode errorCode) {
		return switch (errorCode) {
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
			case DUPLICATE -> HttpStatus.CONFLICT;
			case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
		};
	}
}
