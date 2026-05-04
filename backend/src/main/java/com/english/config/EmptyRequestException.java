package com.english.config;

public class EmptyRequestException extends RuntimeException {
    public EmptyRequestException(String message) {
        super(message);
    }
}
