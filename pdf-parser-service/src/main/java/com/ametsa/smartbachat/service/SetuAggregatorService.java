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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with Setu Account Aggregator APIs.
 * Handles consent creation, data session management, and data fetching.
 */
@Service
public class SetuAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(SetuAggregatorService.class);
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

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
        HttpRequest httpRequest = newHttpRequestBuilder("/v2/consents/" + consentId)
                .GET()
                .build();

        return sendRequest(httpRequest, SetuConsentResponse.class, "get consent status");
    }

    /**
     * Create a data session to fetch financial data.
     */
    public SetuDataSessionResponse createDataSession(String consentId, LocalDate fromDate, LocalDate toDate) {
        
        SetuDataSessionRequest request = new SetuDataSessionRequest();
        request.setConsentId(consentId);
        request.setFormat("json");
        
        SetuDataSessionRequest.DataRange dataRange = new SetuDataSessionRequest.DataRange();
        dataRange.setFrom(fromDate.atStartOfDay().format(ISO_DATE_FORMAT));
        dataRange.setTo(toDate.atTime(LocalTime.MAX).format(ISO_DATE_FORMAT));
        request.setDataRange(dataRange);

        log.info("Creating data session for consent: {}", consentId);
        HttpRequest httpRequest = newHttpRequestBuilder("/v2/sessions")
                .header("Content-Type", "application/json")
                .POST(buildRequestBody(request))
                .build();

        return sendRequest(httpRequest, SetuDataSessionResponse.class, "create data session");
    }

    /**
     * Fetch financial data from a completed session.
     */
    public SetuFIDataResponse fetchSessionData(String sessionId) {
        log.info("Fetching data for session: {}", sessionId);
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
        dataRange.setTo(toDate.atTime(LocalTime.MAX).format(ISO_DATE_FORMAT));
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
     * Sends an HTTP request and handles the response.
     * @param request The HttpRequest to send.
     * @param responseType The class of the expected response object.
     * @param action A description of the action being performed, for logging.
     * @return The deserialized response object.
     */
    private <T> T sendRequest(HttpRequest request, Class<T> responseType, String action) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (responseType == Void.class) return null;
                return objectMapper.readValue(response.body(), responseType);
            } else {
                log.error("Failed to {}: {} - {}", action, response.statusCode(), response.body());
                throw new SetuApiException("Failed to " + action + ": " + response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            log.error("HTTP request interrupted for {}: {}", action, e.getMessage());
            throw new SetuApiException("HTTP request interrupted for " + action, e);
        } catch (SetuApiException e) {
            throw e; // Re-throw our own exceptions
        } catch (Exception e) {
            log.error("Error during HTTP request to {}: {}", action, e.getMessage());
            throw new SetuApiException("Error during HTTP request to " + action, e);
        }
    }
}
