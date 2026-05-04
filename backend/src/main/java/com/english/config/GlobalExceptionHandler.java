package com.english.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE", e.getMessage()));
    }

    @ExceptionHandler(EmptyRequestException.class)
    public ResponseEntity<ErrorResponse> handleEmptyRequest(EmptyRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("EMPTY_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(NoWordsException.class)
    public ResponseEntity<ErrorResponse> handleNoWords(NoWordsException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("NO_WORDS", e.getMessage()));
    }

    @ExceptionHandler(NoPatternsException.class)
    public ResponseEntity<ErrorResponse> handleNoPatterns(NoPatternsException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("NO_PATTERNS", e.getMessage()));
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(InvalidImageException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_IMAGE_FORMAT", e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<ErrorResponse> handleGemini(GeminiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("AI_SERVICE_ERROR", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
    }
}
