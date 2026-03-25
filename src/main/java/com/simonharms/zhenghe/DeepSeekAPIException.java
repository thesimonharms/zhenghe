package com.simonharms.zhenghe;

/**
 * Custom exception class for handling DeepSeek API-specific errors.
 * This exception is thrown when API requests fail or when responses cannot be processed.
 */
public class DeepSeekAPIException extends Exception {

    private final int statusCode;

    /**
     * Constructs a new DeepSeek API exception with the specified detail message.
     *
     * @param message the detail message explaining the error
     */
    public DeepSeekAPIException(String message) {
        super(message);
        this.statusCode = -1;
    }

    /**
     * Constructs a new DeepSeek API exception with an HTTP status code.
     *
     * @param message    the detail message explaining the error
     * @param statusCode the HTTP status code returned by the API
     */
    public DeepSeekAPIException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs a new DeepSeek API exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the error
     * @param cause   the underlying cause of the error
     */
    public DeepSeekAPIException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    /**
     * Returns the HTTP status code associated with this exception, or {@code -1} if not applicable.
     *
     * @return the HTTP status code, or -1 if none
     */
    public int getStatusCode() {
        return statusCode;
    }
}
