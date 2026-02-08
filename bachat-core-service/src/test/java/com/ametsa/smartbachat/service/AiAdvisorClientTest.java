package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.dashboard.ForecastSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAdvisorClient Tests")
class AiAdvisorClientTest {

    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<String> httpResponse;

    private AiAdvisorClient aiAdvisorClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aiAdvisorClient = new AiAdvisorClient(httpClient, objectMapper);
        // Set the base URL using reflection since @Value isn't processed in unit tests
        ReflectionTestUtils.setField(aiAdvisorClient, "baseUrl", "http://localhost:8089");
    }

    @Nested
    @DisplayName("getForecast Tests")
    class GetForecastTests {

        @Test
        void shouldReturnForecastOnSuccessfulResponse() throws Exception {
            String jsonResponse = """
                {
                    "projected_income": 150000.0,
                    "projected_expense": 100000.0,
                    "projected_savings": 50000.0,
                    "trend": "UP",
                    "change_percent": 5.5,
                    "avg_monthly_income": 145000.0,
                    "avg_monthly_expense": 98000.0,
                    "savings_rate": 33.3,
                    "confidence_score": 0.85,
                    "forecast_method": "BLENDED_PROJECTION",
                    "insights": ["Income trending up", "Expenses stable"]
                }
                """;

            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn(jsonResponse);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            Optional<ForecastSummary> result = aiAdvisorClient.getForecast("test-token");

            assertTrue(result.isPresent());
            ForecastSummary forecast = result.get();
            assertEquals(150000.0, forecast.getProjectedIncome());
            assertEquals(100000.0, forecast.getProjectedExpense());
            assertEquals(50000.0, forecast.getProjectedSavings());
            assertEquals("UP", forecast.getTrend());
            assertEquals(0.85, forecast.getConfidenceScore());
            assertEquals("BLENDED_PROJECTION", forecast.getForecastMethod());
            assertEquals(2, forecast.getInsights().size());
        }

        @Test
        void shouldReturnEmptyOnErrorResponse() throws Exception {
            when(httpResponse.statusCode()).thenReturn(500);
            when(httpResponse.body()).thenReturn("Internal Server Error");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            Optional<ForecastSummary> result = aiAdvisorClient.getForecast("test-token");

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnEmptyOnConnectionError() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new java.net.ConnectException("Connection refused"));

            Optional<ForecastSummary> result = aiAdvisorClient.getForecast("test-token");

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnEmptyOnTimeout() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new java.net.http.HttpTimeoutException("Request timed out"));

            Optional<ForecastSummary> result = aiAdvisorClient.getForecast("test-token");

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnEmptyOnInvalidJson() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn("invalid json");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            Optional<ForecastSummary> result = aiAdvisorClient.getForecast("test-token");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("isAvailable Tests")
    class IsAvailableTests {

        @Test
        void shouldReturnTrueWhenHealthCheckSucceeds() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            boolean result = aiAdvisorClient.isAvailable();

            assertTrue(result);
        }

        @Test
        void shouldReturnFalseWhenHealthCheckFails() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new java.net.ConnectException("Connection refused"));

            boolean result = aiAdvisorClient.isAvailable();

            assertFalse(result);
        }
    }
}

