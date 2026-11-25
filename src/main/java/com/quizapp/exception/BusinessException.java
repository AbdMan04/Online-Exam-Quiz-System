package com.quizapp.exception;

/**
 * Thrown when a business rule is violated. The message is shown to the user as-is.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
