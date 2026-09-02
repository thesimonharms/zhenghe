package com.simonharms.zhenghe;

/**
 * Unified exception for provider-level errors.
 *
 * <p>Wraps HTTP errors, deserialization failures, and other issues that
 * can arise when communicating with any AI provider. The HTTP status code
 * and provider name are available for error handling (e.g., retrying on 429).
 */
public class ProviderException extends Exception {

    private final int statusCode;
    private final String providerName;

    /**
     * Constructs a provider exception with a detail message.
     *
     * @param message the detail message
     */
    public ProviderException(String message) {
        super(message);
        this.statusCode = -1;
        this.providerName = null;
    }

    /**
     * Constructs a provider exception with a detail message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public ProviderException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.providerName = null;
    }

    /**
     * Constructs a provider exception with an HTTP status code and cause.
     *
     * @param message    the detail message
     * @param cause      the underlying cause
     * @param statusCode the HTTP status code, or -1 if not applicable
     */
    public ProviderException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.providerName = null;
    }

    /**
     * Constructs a provider exception with all fields.
     *
     * @param message      the detail message
     * @param cause        the underlying cause
     * @param statusCode   the HTTP status code, or -1 if not applicable
     * @param providerName the provider name (may be null)
     */
    public ProviderException(
        String message,
        Throwable cause,
        int statusCode,
        String providerName
    ) {
        super(message, cause);
        this.statusCode = statusCode;
        this.providerName = providerName;
    }

    /**
     * Returns the HTTP status code, or {@code -1} if not applicable.
     *
     * @return the HTTP status code, or -1
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the provider name, or {@code null} if not set.
     *
     * @return the provider name
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Returns {@code true} if the request hit the rate limit (HTTP 429).
     *
     * @return true if status code is 429
     */
    public boolean isRateLimited() {
        return statusCode == 429;
    }
}
