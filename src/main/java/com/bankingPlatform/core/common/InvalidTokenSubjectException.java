package com.bankingPlatform.core.common;

public class InvalidTokenSubjectException extends RuntimeException {
    public InvalidTokenSubjectException(String message) {
        super(message);
    }
}
