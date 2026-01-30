package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.config.SetuConfig;
import com.ametsa.smartbachat.dto.setu.SetuConsentResponse;
import com.ametsa.smartbachat.dto.setu.SetuDataSessionResponse;
import com.ametsa.smartbachat.exception.SetuApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetuAggregatorServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private SetuAggregatorService service;
    private ObjectMapper objectMapper;
    private SetuConfig setuConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        setuConfig = createSetuConfig();
        service = new SetuAggregatorService(setuConfig, objectMapper, httpClient);
    }

    @Nested
    @DisplayName("Create Consent Tests")
    class CreateConsentTests {

        @Test
        void shouldCreateConsentSuccessfully() throws Exception {
            String responseJson = """
                {
                    "id": "consent-123",
                    "url": "https://setu.co/consent/consent-123",
                    "status": "PENDING"
                }
                """;

            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn(responseJson);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            LocalDate fromDate = LocalDate.now().minusMonths(12);
            LocalDate toDate = LocalDate.now();
            SetuConsentResponse response = service.createConsent("9876543210", fromDate, toDate);

            assertNotNull(response);
            assertEquals("consent-123", response.getId());
            assertEquals("https://setu.co/consent/consent-123", response.getUrl());
            verify(httpClient, times(1)).send(any(), any());
        }

        @Test
        void shouldThrowExceptionOnConsentFailure() throws Exception {
            when(httpResponse.statusCode()).thenReturn(400);
            when(httpResponse.body()).thenReturn("{\"error\": \"Invalid mobile number\"}");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            LocalDate fromDate = LocalDate.now().minusMonths(12);
            LocalDate toDate = LocalDate.now();
            assertThrows(SetuApiException.class, () ->
                    service.createConsent("invalid", fromDate, toDate));
        }
    }

    @Nested
    @DisplayName("Create Data Session Tests")
    class CreateDataSessionTests {

        @Test
        void shouldCreateDataSessionSuccessfully() throws Exception {
            String responseJson = """
                {
                    "id": "session-456",
                    "status": "PENDING",
                    "format": "json"
                }
                """;

            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn(responseJson);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            SetuDataSessionResponse response = service.createDataSession(
                    "consent-123", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

            assertNotNull(response);
            assertEquals("session-456", response.getId());
        }
    }

    @Nested
    @DisplayName("Retry Logic Tests")
    class RetryLogicTests {

        @Test
        void shouldRetryOn500Error() throws Exception {
            String successJson = """
                {"id": "consent-123", "url": "https://setu.co/consent", "status": "PENDING"}
                """;

            // First call returns 500, second returns 200
            when(httpResponse.statusCode()).thenReturn(500).thenReturn(200);
            when(httpResponse.body()).thenReturn("Server Error").thenReturn(successJson);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            LocalDate fromDate = LocalDate.now().minusMonths(12);
            LocalDate toDate = LocalDate.now();
            SetuConsentResponse response = service.createConsent("9876543210", fromDate, toDate);

            assertNotNull(response);
            verify(httpClient, times(2)).send(any(), any());
        }

        @Test
        void shouldRetryOn503Error() throws Exception {
            String successJson = """
                {"id": "consent-123", "url": "https://setu.co/consent", "status": "PENDING"}
                """;

            when(httpResponse.statusCode()).thenReturn(503).thenReturn(200);
            when(httpResponse.body()).thenReturn("Service Unavailable").thenReturn(successJson);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            LocalDate fromDate = LocalDate.now().minusMonths(12);
            LocalDate toDate = LocalDate.now();
            SetuConsentResponse response = service.createConsent("9876543210", fromDate, toDate);

            assertNotNull(response);
            verify(httpClient, times(2)).send(any(), any());
        }

        @Test
        void shouldFailAfterMaxRetries() throws Exception {
            when(httpResponse.statusCode()).thenReturn(500);
            when(httpResponse.body()).thenReturn("Server Error");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            LocalDate fromDate = LocalDate.now().minusMonths(12);
            LocalDate toDate = LocalDate.now();
            assertThrows(SetuApiException.class, () ->
                    service.createConsent("9876543210", fromDate, toDate));

            // Should have tried 3 times (max retries)
            verify(httpClient, times(3)).send(any(), any());
        }

        @Test
        void shouldNotRetryOn400ClientError() throws Exception {
            when(httpResponse.statusCode()).thenReturn(400);
            when(httpResponse.body()).thenReturn("Bad Request");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            LocalDate fromDate = LocalDate.now().minusMonths(12);
            LocalDate toDate = LocalDate.now();
            assertThrows(SetuApiException.class, () ->
                    service.createConsent("9876543210", fromDate, toDate));

            // Should NOT retry on 400
            verify(httpClient, times(1)).send(any(), any());
        }
    }

    @Nested
    @DisplayName("Get Consent Status Tests")
    class GetConsentStatusTests {

        @Test
        void shouldGetConsentStatusSuccessfully() throws Exception {
            String responseJson = """
                {"id": "consent-123", "status": "ACTIVE"}
                """;

            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn(responseJson);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            SetuConsentResponse response = service.getConsentStatus("consent-123");

            assertNotNull(response);
            assertEquals("ACTIVE", response.getStatus());
        }
    }

    private SetuConfig createSetuConfig() {
        SetuConfig config = new SetuConfig();
        config.setBaseUrl("https://fiu-sandbox.setu.co");
        config.setClientId("test-client-id");
        config.setClientSecret("test-client-secret");
        config.setProductInstanceId("test-product-id");
        config.setRedirectUrl("http://localhost:3000/callback");
        config.setConsentDurationMonths(12);
        config.setDataFetchMonths(12);
        config.setFiTypes("DEPOSIT");
        config.setVuaSuffix("@onemoney");
        return config;
    }
}