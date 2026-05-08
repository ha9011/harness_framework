package com.english.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		return ResponseEntity.badRequest()
				.body(new ApiErrorResponse("validation_error", "Invalid request."));
	}

	@ExceptionHandler(ErrorResponseException.class)
	public ResponseEntity<ApiErrorResponse> handleErrorResponse(ErrorResponseException exception) {
		HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(status.name().toLowerCase(), exception.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("internal_server_error", "Unexpected server error."));
	}
}
