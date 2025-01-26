package com.simonharms.zhenghe;

// Import necessary libraries
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for making HTTP requests to the DeepSeek API.
 * Handles authentication, request formatting, and response parsing.
 */
public class DeepSeekAPIClient {
    // Private fields to store API key, base URL, HTTP client, and JSON mapper
    private final String apiKey; // API key for authentication
    private final String baseUrl; // Base URL of the API (e.g., "https://api.deepseek.com/v1/")
    private final OkHttpClient httpClient; // OkHttp client for making HTTP requests
    private final ObjectMapper objectMapper; // Jackson ObjectMapper for JSON processing
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekAPIClient.class); // Logger object

    /**
     * Constructs a new DeepSeek API client with the specified credentials and configuration.
     *
     * @param apiKey the API key for authentication with DeepSeek services
     * @param baseUrl the base URL of the DeepSeek API
     */
    public DeepSeekAPIClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey; // Set the API key
        this.baseUrl = baseUrl; // Set the base URL
        // Configure OkHttp with longer timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)    // Increased from 30 to 60
                .readTimeout(90, TimeUnit.SECONDS)       // Increased from 30 to 90
                .writeTimeout(60, TimeUnit.SECONDS)      // Increased from 30 to 60
                .retryOnConnectionFailure(true)          // Added retry
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // Add this line
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * Sends a GET request to the specified API endpoint.
     *
     * @param <T> the type of the expected response
     * @param endpoint the API endpoint to send the request to
     * @param responseType the class object representing the expected response type
     * @return the deserialized response object
     * @throws IOException if the request fails or the response cannot be processed
     */
    public <T> T sendGetRequest(String endpoint, Class<T> responseType) throws IOException {
        // Build the URL by combining the base URL and the endpoint
        String url = baseUrl + endpoint;

        logger.debug("Preparing GET request to endpoint: {}", endpoint);

        logger.info("Sending GET request to: {}", url);

        // Create a new HTTP GET request using OkHttp's Request.Builder
        Request request = new Request.Builder()
                .url(url) // Set the URL
                .header("Authorization", "Bearer " + apiKey) // Add the Authorization header
                .header("Accept", "application/json") // Specify that we accept JSON responses
                .get() // Set the request method to GET
                .build(); // Build the request

        // Execute the request and get the response
        try (Response response = httpClient.newCall(request).execute()) {
            // Check if the response is successful (HTTP status code 200-299)
            if (response.isSuccessful() && response.body() != null) {
                // Read the response body as a string
                String responseBody = response.body().string();

                // Use Jackson to convert the JSON response into a Java object of the specified type
                return objectMapper.readValue(responseBody, responseType);
            } else {
                // If the request fails, throw an exception with the error details
                throw new IOException("GET request failed. Code: " + response.code() + " - " + response.message());
            }
        }
    }

    /**
     * Sends a POST request to the specified API endpoint with a request body.
     *
     * @param <T> the type of the expected response
     * @param <R> the type of the request body
     * @param endpoint the API endpoint to send the request to
     * @param requestBody the object to be sent as the request body
     * @param responseType the class object representing the expected response type
     * @return the deserialized response object
     * @throws IOException if the request fails or the response cannot be processed
     */
    public <T, R> T sendPostRequest(String endpoint, R requestBody, Class<T> responseType) throws IOException {
        // Convert the request body (a Java object) into a JSON string using Jackson
        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        // Build the URL by combining the base URL and the endpoint
        String url = baseUrl + endpoint;

        logger.debug("Preparing POST request to endpoint: {}", endpoint);

        logger.info("Sending POST request to: {}", url);
        logger.debug("Request payload:\n{}", jsonPayload);


        // Create a new HTTP POST request using OkHttp's Request.Builder
        RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json")); // Create the request body
        Request request = new Request.Builder()
                .url(url) // Set the URL
                .header("Authorization", "Bearer " + apiKey) // Add the Authorization header
                .header("Content-Type", "application/json") // Specify that we're sending JSON
                .header("Accept", "application/json") // Specify that we accept JSON responses
                .post(body) // Set the request method to POST and include the body
                .build(); // Build the request

        // Execute the request and get the response
        try (Response response = httpClient.newCall(request).execute()) {
            // Check if the response is successful (HTTP status code 200-299)
            String responseBody = response.body() != null ? response.body().string() : null;

            logger.debug("Received response code: {}", response.code());
            logger.trace("Raw response body:\n{}", responseBody);

            if (response.isSuccessful() && responseBody != null) {
                // Log successful API call
                logger.info("Request successful to {}", url);

                // Read the response body as a string
                // Use Jackson to convert the JSON response into a Java object of the specified type
                return objectMapper.readValue(responseBody, responseType);
            } else {
                // Log failed API call
                logger.error("Request failed - Code: {} - Message: {}", response.code(), response.message());
                logger.debug("Error response body:\n{}", responseBody);
                // If the request fails, throw an exception with the error details
                throw new IOException("POST request failed. Code: " + response.code() 
                    + " - " + response.message() 
                    + "\nResponse body: " + responseBody);
            }
        }
    }
}