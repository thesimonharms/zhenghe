package com.simonharms.zhenghe;

import java.io.IOException;

/**
 * An {@link IOException} that carries the HTTP status code and response body
 * from a failed DeepSeek API call.
 *
 * <p>Thrown by {@link DeepSeekAPIClient}. Because it extends {@link IOException},
 * existing code that catches {@code IOException} keeps working. Catch this type
 * first to read the HTTP status, e.g. to handle 429 rate limits separately.
 *
 * <p>Documented DeepSeek API status codes:
 *
 * <ul>
 *   <li>400 — invalid request body format</li>
 *   <li>401 — authentication failed (wrong API key)</li>
 *   <li>402 — insufficient balance</li>
 *   <li>422 — invalid parameters</li>
 *   <li>429 — rate limit reached</li>
 *   <li>500 — server error</li>
 *   <li>503 — server overloaded</li>
 * </ul>
 */
public class DeepSeekHTTPException extends IOException {

    private final int statusCode;
    private final transient String responseBody;

    /**
     * Constructs an HTTP exception with the status code and response body.
     *
     * @param message       the detail message explaining the error
     * @param statusCode    the HTTP status code returned by the API
     * @param responseBody  the raw error response body (may be null)
     */
    public DeepSeekHTTPException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Returns the HTTP status code returned by the API.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the raw response body sent with the error, if any.
     *
     * @return the error body, or null
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Returns {@code true} if the request hit the documented rate limit (429).
     *
     * @return true if the status code is 429
     */
    public boolean isRateLimited() {
        return statusCode == 429;
    }
}
