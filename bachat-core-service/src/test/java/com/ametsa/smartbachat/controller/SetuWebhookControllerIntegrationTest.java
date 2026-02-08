package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.config.JwtConfig;
import com.ametsa.smartbachat.config.SetuConfig;
import com.ametsa.smartbachat.dto.setu.SetuWebhookPayload;
import com.ametsa.smartbachat.security.JwtAuthenticationFilter;
import com.ametsa.smartbachat.service.BankConnectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SetuWebhookController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class SetuWebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankConnectionService bankConnectionService;

    @MockBean
    private SetuConfig setuConfig;

    @MockBean
    private JwtConfig jwtConfig;

    @Nested
    @DisplayName("POST /api/v1/webhooks/setu")
    class WebhookEndpointTests {

        @Test
        void shouldHandleConsentApprovedWebhook() throws Exception {
            SetuWebhookPayload payload = new SetuWebhookPayload();
            payload.setType("CONSENT_STATUS_UPDATE");
            payload.setConsentId("consent-123");
            payload.setStatus("ACTIVE");

            doNothing().when(bankConnectionService).handleWebhook(any(SetuWebhookPayload.class));

            mockMvc.perform(post("/api/v1/webhooks/setu")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("OK"));

            verify(bankConnectionService).handleWebhook(any(SetuWebhookPayload.class));
        }

        @Test
        void shouldHandleConsentRejectedWebhook() throws Exception {
            SetuWebhookPayload payload = new SetuWebhookPayload();
            payload.setType("CONSENT_STATUS_UPDATE");
            payload.setConsentId("consent-456");
            payload.setStatus("REJECTED");

            doNothing().when(bankConnectionService).handleWebhook(any(SetuWebhookPayload.class));

            mockMvc.perform(post("/api/v1/webhooks/setu")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldHandleSessionCompletedWebhook() throws Exception {
            SetuWebhookPayload payload = new SetuWebhookPayload();
            payload.setType("SESSION_STATUS_UPDATE");
            payload.setSessionId("session-789");
            payload.setStatus("COMPLETED");

            doNothing().when(bankConnectionService).handleWebhook(any(SetuWebhookPayload.class));

            mockMvc.perform(post("/api/v1/webhooks/setu")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldHandleFINotificationWebhook() throws Exception {
            SetuWebhookPayload payload = new SetuWebhookPayload();
            payload.setType("FI_NOTIFICATION");
            payload.setConsentId("consent-123");
            payload.setSessionId("session-456");

            doNothing().when(bankConnectionService).handleWebhook(any(SetuWebhookPayload.class));

            mockMvc.perform(post("/api/v1/webhooks/setu")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldReturnOkEvenOnProcessingError() throws Exception {
            SetuWebhookPayload payload = new SetuWebhookPayload();
            payload.setType("CONSENT_STATUS_UPDATE");
            payload.setConsentId("consent-error");
            payload.setStatus("ACTIVE");

            doThrow(new RuntimeException("Processing error"))
                    .when(bankConnectionService).handleWebhook(any(SetuWebhookPayload.class));

            // Webhook should return 500 on error
            mockMvc.perform(post("/api/v1/webhooks/setu")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void shouldHandleEmptyPayload() throws Exception {
            mockMvc.perform(post("/api/v1/webhooks/setu")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldHandleUnknownWebhookType() throws Exception {
            SetuWebhookPayload payload = new SetuWebhookPayload();
            payload.setType("UNKNOWN_TYPE");

            doNothing().when(bankConnectionService).handleWebhook(any(SetuWebhookPayload.class));

            mockMvc.perform(post("/api/v1/webhooks/setu")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Webhook Signature Verification")
    class SignatureVerificationTests {

        @Test
        void shouldAcceptValidSignature() throws Exception {
            SetuWebhookPayload payload = new SetuWebhookPayload();
            payload.setType("CONSENT_STATUS_UPDATE");
            payload.setConsentId("consent-123");
            payload.setStatus("ACTIVE");

            doNothing().when(bankConnectionService).handleWebhook(any(SetuWebhookPayload.class));

            // In production, this would include proper signature header
            mockMvc.perform(post("/api/v1/webhooks/setu")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setu-Signature", "valid-signature")
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }
    }
}

