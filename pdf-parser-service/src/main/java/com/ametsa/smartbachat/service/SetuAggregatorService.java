package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.config.SetuConfig;
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

    public SetuAggregatorService(SetuConfig setuConfig, ObjectMapper objectMapper) {
        this.setuConfig = setuConfig;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Create a consent request for the user.
     * @param mobileNumber User's mobile number (will be formatted as VUA)
     * @param dataFromDate Start date for data range
     * @param dataToDate End date for data range
     * @return Consent response with redirect URL
     */
    public SetuConsentResponse createConsent(String mobileNumber, LocalDate dataFromDate, LocalDate dataToDate) 
            throws Exception {
        
        SetuConsentRequest request = buildConsentRequest(mobileNumber, dataFromDate, dataToDate);
        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(setuConfig.getBaseUrl() + "/v2/consents"))
                .header("Content-Type", "application/json")
                .header("x-client-id", setuConfig.getClientId())
                .header("x-client-secret", setuConfig.getClientSecret())
                .header("x-product-instance-id", setuConfig.getProductInstanceId())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        log.info("Creating consent for mobile: {}****", mobileNumber.substring(0, 4));
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), SetuConsentResponse.class);
        } else {
            log.error("Consent creation failed: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Failed to create consent: " + response.body());
        }
    }

    /**
     * Get consent status by consent ID.
     */
    public SetuConsentResponse getConsentStatus(String consentId) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(setuConfig.getBaseUrl() + "/v2/consents/" + consentId))
                .header("x-client-id", setuConfig.getClientId())
                .header("x-client-secret", setuConfig.getClientSecret())
                .header("x-product-instance-id", setuConfig.getProductInstanceId())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), SetuConsentResponse.class);
        } else {
            log.error("Get consent status failed: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Failed to get consent status: " + response.body());
        }
    }

    /**
     * Create a data session to fetch financial data.
     */
    public SetuDataSessionResponse createDataSession(String consentId, LocalDate fromDate, LocalDate toDate) 
            throws Exception {
        
        SetuDataSessionRequest request = new SetuDataSessionRequest();
        request.setConsentId(consentId);
        request.setFormat("json");
        
        SetuDataSessionRequest.DataRange dataRange = new SetuDataSessionRequest.DataRange();
        dataRange.setFrom(fromDate.atStartOfDay().format(ISO_DATE_FORMAT));
        dataRange.setTo(toDate.atTime(23, 59, 59).format(ISO_DATE_FORMAT));
        request.setDataRange(dataRange);

        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(setuConfig.getBaseUrl() + "/v2/sessions"))
                .header("Content-Type", "application/json")
                .header("x-client-id", setuConfig.getClientId())
                .header("x-client-secret", setuConfig.getClientSecret())
                .header("x-product-instance-id", setuConfig.getProductInstanceId())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        log.info("Creating data session for consent: {}", consentId);
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), SetuDataSessionResponse.class);
        } else {
            log.error("Data session creation failed: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Failed to create data session: " + response.body());
        }
    }

    /**
     * Fetch financial data from a completed session.
     */
    public SetuFIDataResponse fetchSessionData(String sessionId) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(setuConfig.getBaseUrl() + "/v2/sessions/" + sessionId))
                .header("x-client-id", setuConfig.getClientId())
                .header("x-client-secret", setuConfig.getClientSecret())
                .header("x-product-instance-id", setuConfig.getProductInstanceId())
                .GET()
                .build();

        log.info("Fetching data for session: {}", sessionId);
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), SetuFIDataResponse.class);
        } else {
            log.error("Fetch session data failed: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Failed to fetch session data: " + response.body());
        }
    }

    /**
     * Build consent request with proper structure.
     */
    private SetuConsentRequest buildConsentRequest(String mobileNumber, LocalDate fromDate, LocalDate toDate) {
        SetuConsentRequest request = new SetuConsentRequest();

        // Format VUA: mobile@setu-aa (or appropriate AA handle)
        request.setVua(mobileNumber + "@setu-aa");

        // Consent duration
        SetuConsentRequest.ConsentDuration duration = new SetuConsentRequest.ConsentDuration();
        duration.setUnit("MONTH");
        duration.setValue(setuConfig.getConsentDurationMonths());
        request.setConsentDuration(duration);

        // Data range
        SetuConsentRequest.DataRange dataRange = new SetuConsentRequest.DataRange();
        dataRange.setFrom(fromDate.atStartOfDay().format(ISO_DATE_FORMAT));
        dataRange.setTo(toDate.atTime(23, 59, 59).format(ISO_DATE_FORMAT));
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
    public void revokeConsent(String consentId) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(setuConfig.getBaseUrl() + "/v2/consents/" + consentId))
                .header("x-client-id", setuConfig.getClientId())
                .header("x-client-secret", setuConfig.getClientSecret())
                .header("x-product-instance-id", setuConfig.getProductInstanceId())
                .DELETE()
                .build();

        log.info("Revoking consent: {}", consentId);

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Consent revocation failed: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Failed to revoke consent: " + response.body());
        }

        log.info("Consent revoked successfully: {}", consentId);
    }
}

