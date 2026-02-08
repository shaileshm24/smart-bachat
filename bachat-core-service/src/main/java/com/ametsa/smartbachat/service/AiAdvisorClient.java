package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.dashboard.ForecastSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Client for communicating with the AI Advisor microservice.
 * Provides AI-powered financial insights and forecasting.
 */
@Service
public class AiAdvisorClient {

    private static final Logger log = LoggerFactory.getLogger(AiAdvisorClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    @Value("${ai-advisor.base-url:http://localhost:8089}")
    private String baseUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiAdvisorClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Get AI-powered financial forecast from the AI Advisor service.
     *
     * @param authToken The JWT token for authentication
     * @return Optional containing the forecast, or empty if service unavailable
     */
    public Optional<ForecastSummary> getForecast(String authToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/advisor/forecast"))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            log.debug("Calling AI Advisor forecast endpoint: {}", baseUrl + "/api/advisor/forecast");

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                ForecastSummary forecast = objectMapper.readValue(response.body(), ForecastSummary.class);
                log.info("Received AI forecast: method={}, confidence={}",
                        forecast.getForecastMethod(), forecast.getConfidenceScore());
                return Optional.of(forecast);
            } else {
                log.warn("AI Advisor returned error: {} - {}", response.statusCode(), response.body());
                return Optional.empty();
            }
        } catch (java.net.ConnectException e) {
            log.warn("AI Advisor service unavailable (connection refused): {}", e.getMessage());
            return Optional.empty();
        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("AI Advisor service timeout: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error calling AI Advisor service: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Check if the AI Advisor service is available.
     */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("AI Advisor health check failed: {}", e.getMessage());
            return false;
        }
    }
}

