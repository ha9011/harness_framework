package com.english.config;

import com.english.auth.AuthErrorCode;
import com.english.auth.AuthException;
import com.english.generate.GenerateErrorCode;
import com.english.generate.GenerateException;
import com.english.pattern.PatternErrorCode;
import com.english.pattern.PatternException;
import com.english.review.ReviewErrorCode;
import com.english.review.ReviewException;
import com.english.word.WordErrorCode;
import com.english.word.WordException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		return ResponseEntity.badRequest()
				.body(new ApiErrorResponse("BAD_REQUEST", "요청 값이 올바르지 않습니다"));
	}

	@ExceptionHandler({
			HandlerMethodValidationException.class,
			ConstraintViolationException.class,
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class
	})
	public ResponseEntity<ApiErrorResponse> handleRequestValidation(Exception exception) {
		return ResponseEntity.badRequest()
				.body(new ApiErrorResponse("BAD_REQUEST", "요청 값이 올바르지 않습니다"));
	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ApiErrorResponse> handleAuth(AuthException exception) {
		HttpStatus status = statusOf(exception.getErrorCode());
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(exception.getErrorCode().name(), exception.getMessage()));
	}

	@ExceptionHandler(WordException.class)
	public ResponseEntity<ApiErrorResponse> handleWord(WordException exception) {
		HttpStatus status = statusOf(exception.getErrorCode());
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(exception.getErrorCode().name(), exception.getMessage()));
	}

	@ExceptionHandler(PatternException.class)
	public ResponseEntity<ApiErrorResponse> handlePattern(PatternException exception) {
		HttpStatus status = statusOf(exception.getErrorCode());
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(exception.getErrorCode().name(), exception.getMessage()));
	}

	@ExceptionHandler(GenerateException.class)
	public ResponseEntity<ApiErrorResponse> handleGenerate(GenerateException exception) {
		HttpStatus status = statusOf(exception.getErrorCode());
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(exception.getErrorCode().name(), exception.getMessage()));
	}

	@ExceptionHandler(ReviewException.class)
	public ResponseEntity<ApiErrorResponse> handleReview(ReviewException exception) {
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
		log.error("Unexpected error: {}", exception.getMessage(), exception);
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

	private static HttpStatus statusOf(WordErrorCode errorCode) {
		return switch (errorCode) {
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
			case DUPLICATE -> HttpStatus.CONFLICT;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
		};
	}

	private static HttpStatus statusOf(PatternErrorCode errorCode) {
		return switch (errorCode) {
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
			case DUPLICATE -> HttpStatus.CONFLICT;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
		};
	}

	private static HttpStatus statusOf(GenerateErrorCode errorCode) {
		return switch (errorCode) {
			case BAD_REQUEST, INVALID_IMAGE_FORMAT, NO_WORDS, NO_PATTERNS -> HttpStatus.BAD_REQUEST;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
			case AI_SERVICE_ERROR -> HttpStatus.BAD_GATEWAY;
		};
	}

	private static HttpStatus statusOf(ReviewErrorCode errorCode) {
		return switch (errorCode) {
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
		};
	}
}
