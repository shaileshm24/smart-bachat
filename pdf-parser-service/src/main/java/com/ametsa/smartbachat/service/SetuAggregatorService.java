package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.config.SetuConfig;
import com.ametsa.smartbachat.exception.SetuApiException;
import com.ametsa.smartbachat.dto.setu.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for interacting with Setu Account Aggregator APIs.
 * Handles consent creation, data session management, and data fetching.
 */
@Service
public class SetuAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(SetuAggregatorService.class);
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0;

    // HTTP status codes that should trigger a retry
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(408, 429, 500, 502, 503, 504);

    private final SetuConfig setuConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SetuAggregatorService(SetuConfig setuConfig, ObjectMapper objectMapper, HttpClient httpClient) {
        this.setuConfig = setuConfig;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * Create a consent request for the user.
     * @param mobileNumber User's mobile number (will be formatted as VUA)
     * @param dataFromDate Start date for data range
     * @param dataToDate End date for data range
     * @return Consent response with redirect URL
     */
    public SetuConsentResponse createConsent(String mobileNumber, LocalDate dataFromDate, LocalDate dataToDate) {
        
        SetuConsentRequest request = buildConsentRequest(mobileNumber, dataFromDate, dataToDate);

        log.info("Creating consent for mobile: {}****", mobileNumber.substring(0, 4));
        HttpRequest httpRequest = newHttpRequestBuilder("/v2/consents")
                .header("Content-Type", "application/json")
                .POST(buildRequestBody(request))
                .build();

        return sendRequest(httpRequest, SetuConsentResponse.class, "create consent");
    }

    /**
     * Get consent status by consent ID.
     */
    public SetuConsentResponse getConsentStatus(String consentId) {
        return getConsentStatus(consentId, false);
    }

    /**
     * Get consent status by consent ID with optional expanded details.
     */
    public SetuConsentResponse getConsentStatus(String consentId, boolean expanded) {
        String path = "/v2/consents/" + consentId;
        if (expanded) {
            path += "?expanded=true";
        }
        HttpRequest httpRequest = newHttpRequestBuilder(path)
                .GET()
                .build();

        return sendRequest(httpRequest, SetuConsentResponse.class, "get consent status");
    }

    /**
     * Create a data session to fetch financial data.
     */
    public SetuDataSessionResponse createDataSession(String consentId, LocalDate fromDate, LocalDate toDate) {

        // Ensure fromDate is before toDate
        if (fromDate.isAfter(toDate)) {
            log.warn("fromDate {} is after toDate {}, swapping dates", fromDate, toDate);
            LocalDate temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }

        SetuDataSessionRequest request = new SetuDataSessionRequest();
        request.setConsentId(consentId);
        request.setFormat("json");

        SetuDataSessionRequest.DataRange dataRange = new SetuDataSessionRequest.DataRange();
        // Use consistent time format: start of day for from, end of day for to
        String fromDateStr = fromDate.atStartOfDay().format(ISO_DATE_FORMAT);
        String toDateStr = toDate.atTime(23, 59, 59, 999000000).format(ISO_DATE_FORMAT);
        dataRange.setFrom(fromDateStr);
        dataRange.setTo(toDateStr);
        request.setDataRange(dataRange);

        log.info("Creating data session for consent: {} with range from={} to={}", consentId, fromDateStr, toDateStr);

        // Log the request body for debugging
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            log.info("Data session request body: {}", requestJson);
        } catch (Exception e) {
            log.warn("Could not log request body", e);
        }

        HttpRequest httpRequest = newHttpRequestBuilder("/v2/sessions")
                .header("Content-Type", "application/json")
                .POST(buildRequestBody(request))
                .build();

        return sendRequest(httpRequest, SetuDataSessionResponse.class, "create data session");
    }

    /**
     * Fetch financial data from a completed session.
     * Polls until the session is COMPLETED or times out.
     */
    public SetuFIDataResponse fetchSessionData(String sessionId) {
        return fetchSessionDataWithPolling(sessionId, 30, 2000); // 30 attempts, 2 seconds apart = 60 seconds max
    }

    /**
     * Fetch financial data with polling for session completion.
     */
    public SetuFIDataResponse fetchSessionDataWithPolling(String sessionId, int maxAttempts, long pollIntervalMs) {
        log.info("Fetching data for session: {} (will poll up to {} times)", sessionId, maxAttempts);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpRequest httpRequest = newHttpRequestBuilder("/v2/sessions/" + sessionId)
                    .GET()
                    .build();

            SetuFIDataResponse response = sendRequest(httpRequest, SetuFIDataResponse.class, "fetch session data");

            String status = response.getStatus();
            log.info("Session {} status: {} (attempt {}/{})", sessionId, status, attempt, maxAttempts);

            if ("COMPLETED".equalsIgnoreCase(status) || "PARTIAL".equalsIgnoreCase(status)) {
                log.info("Session {} is ready, returning data", sessionId);
                // Log the raw response for debugging and write to file
                HttpRequest rawRequest = newHttpRequestBuilder("/v2/sessions/" + sessionId)
                        .GET()
                        .build();
                try {
                    HttpResponse<String> rawResponse = httpClient.send(rawRequest, HttpResponse.BodyHandlers.ofString());
                    log.info("Session {} COMPLETED raw response: {}", sessionId, rawResponse.body());
                    // Write to file for analysis
                    java.nio.file.Files.writeString(
                            java.nio.file.Path.of("/tmp/setu_session_" + sessionId + ".json"),
                            rawResponse.body());
                    log.info("Wrote raw response to /tmp/setu_session_{}.json", sessionId);
                } catch (Exception e) {
                    log.warn("Could not log raw response", e);
                }
                return response;
            } else if ("FAILED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status)) {
                log.error("Session {} failed with status: {}", sessionId, status);
                return response; // Return as-is, let caller handle
            }

            // Still PENDING, wait and retry
            if (attempt < maxAttempts) {
                log.debug("Session {} still pending, waiting {}ms before retry", sessionId, pollIntervalMs);
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Polling interrupted for session {}", sessionId);
                    return response;
                }
            }
        }

        log.warn("Session {} did not complete within {} attempts", sessionId, maxAttempts);
        // Return the last response (still PENDING)
        HttpRequest httpRequest = newHttpRequestBuilder("/v2/sessions/" + sessionId)
                .GET()
                .build();
        return sendRequest(httpRequest, SetuFIDataResponse.class, "fetch session data");
    }

    /**
     * Build consent request with proper structure.
     */
    private SetuConsentRequest buildConsentRequest(String mobileNumber, LocalDate fromDate, LocalDate toDate) {
        SetuConsentRequest request = new SetuConsentRequest();

        // Format VUA: mobile@setu-aa (or appropriate AA handle from config)
        request.setVua(mobileNumber + setuConfig.getVuaSuffix());

        // Consent duration
        SetuConsentRequest.ConsentDuration duration = new SetuConsentRequest.ConsentDuration();
        duration.setUnit("MONTH");
        duration.setValue(setuConfig.getConsentDurationMonths());
        request.setConsentDuration(duration);

        // Data range
        SetuConsentRequest.DataRange dataRange = new SetuConsentRequest.DataRange();
        dataRange.setFrom(fromDate.atStartOfDay().format(ISO_DATE_FORMAT));
        dataRange.setTo(toDate.atTime(23, 59, 59, 999000000).format(ISO_DATE_FORMAT));
        request.setDataRange(dataRange);

        // Context (optional metadata)
        List<SetuConsentRequest.Context> contextList = new ArrayList<>();
        SetuConsentRequest.Context ctx = new SetuConsentRequest.Context();
        ctx.setKey("accounttype");
        ctx.setValue("SAVINGS");
        contextList.add(ctx);
        request.setContext(contextList);

        return request;
    }

    /**
     * Revoke an existing consent.
     */
    public void revokeConsent(String consentId) {
        log.info("Revoking consent: {}", consentId);
        HttpRequest httpRequest = newHttpRequestBuilder("/v2/consents/" + consentId)
                .DELETE()
                .build();

        sendRequest(httpRequest, Void.class, "revoke consent");
        log.info("Consent revoked successfully for ID: {}", consentId);
    }

    /**
     * Creates a pre-configured HttpRequest.Builder with common headers.
     * @param path The API endpoint path.
     * @return A pre-configured HttpRequest.Builder.
     */
    private HttpRequest.Builder newHttpRequestBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(setuConfig.getBaseUrl() + path))
                .header("x-client-id", setuConfig.getClientId())
                .header("x-client-secret", setuConfig.getClientSecret())
                .header("x-product-instance-id", setuConfig.getProductInstanceId());
    }

    /**
     * Serializes a request object to a JSON string for the request body.
     * @param request The request object.
     * @return The request body publisher.
     */
    private HttpRequest.BodyPublisher buildRequestBody(Object request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            return HttpRequest.BodyPublishers.ofString(requestBody);
        } catch (Exception e) {
            log.error("Failed to serialize request body", e);
            throw new SetuApiException("Failed to serialize request body");
        }
    }

    /**
     * Sends an HTTP request with retry logic and exponential backoff.
     * @param request The HttpRequest to send.
     * @param responseType The class of the expected response object.
     * @param action A description of the action being performed, for logging.
     * @return The deserialized response object.
     */
    private <T> T sendRequest(HttpRequest request, Class<T> responseType, String action) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    if (responseType == Void.class) return null;
                    log.debug("[Setu] Raw response for {}: {}", action, response.body());
                    // Log raw response for session data fetches
                    if (action.contains("session data")) {
                        log.info("[Setu] Session data raw response: {}", response.body());
                    }
                    return objectMapper.readValue(response.body(), responseType);
                } else if (RETRYABLE_STATUS_CODES.contains(statusCode) && attempt < MAX_RETRIES) {
                    log.warn("[Setu] Retryable error for {} (attempt {}/{}): {} - {}",
                            action, attempt, MAX_RETRIES, statusCode, response.body());
                    sleepWithBackoff(backoffMs);
                    backoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
                } else {
                    log.error("Failed to {}: {} - {}", action, statusCode, response.body());
                    throw new SetuApiException("Failed to " + action + ": " + response.body(), statusCode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("HTTP request interrupted for {}: {}", action, e.getMessage());
                throw new SetuApiException("HTTP request interrupted for " + action, e);
            } catch (SetuApiException e) {
                throw e;
            } catch (java.io.IOException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    log.warn("[Setu] IO error for {} (attempt {}/{}): {}",
                            action, attempt, MAX_RETRIES, e.getMessage());
                    sleepWithBackoff(backoffMs);
                    backoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
                } else {
                    log.error("Error during HTTP request to {}: {}", action, e.getMessage());
                    throw new SetuApiException("Error during HTTP request to " + action, e);
                }
            } catch (Exception e) {
                log.error("Error during HTTP request to {}: {}", action, e.getMessage());
                throw new SetuApiException("Error during HTTP request to " + action, e);
            }
        }

        // Should not reach here, but just in case
        throw new SetuApiException("Max retries exceeded for " + action, lastException);
    }

    /**
     * Sleep for the specified duration, handling interruption.
     */
    private void sleepWithBackoff(long millis) {
        try {
            log.debug("[Setu] Waiting {}ms before retry", millis);
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SetuApiException("Retry sleep interrupted", e);
        }
    }
}
