package com.simonharms.zhenghe;

/**
 * Custom exception class for handling DeepSeek API-specific errors.
 * This exception is thrown when API requests fail or when responses cannot be processed.
 */
public class DeepSeekAPIException extends Exception {
    
    /**
     * Constructs a new DeepSeek API exception with the specified detail message.
     *
     * @param message the detail message explaining the error
     */
    public DeepSeekAPIException(String message) {
        super(message);
    }

    /**
     * Constructs a new DeepSeek API exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the error
     * @param cause the underlying cause of the error
     */
    public DeepSeekAPIException(String message, Throwable cause) {
        super(message, cause);
    }
}
