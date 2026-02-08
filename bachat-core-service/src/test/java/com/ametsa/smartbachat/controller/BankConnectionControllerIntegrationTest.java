package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.config.JwtConfig;
import com.ametsa.smartbachat.dto.BankAccountDto;
import com.ametsa.smartbachat.dto.BankConnectionRequestDto;
import com.ametsa.smartbachat.dto.BankConnectionResponseDto;
import com.ametsa.smartbachat.security.JwtAuthenticationFilter;
import com.ametsa.smartbachat.security.UserPrincipal;
import com.ametsa.smartbachat.service.BankConnectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BankConnectionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class BankConnectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankConnectionService bankConnectionService;

    @MockBean
    private JwtConfig jwtConfig;

    private UUID testUserId;
    private UUID testProfileId;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testProfileId = UUID.randomUUID();
        testPrincipal = new UserPrincipal(testUserId, testProfileId, "test@example.com", Collections.emptyList());

        // Set up security context with test principal
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testPrincipal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("POST /api/v1/bank/connect")
    class ConnectEndpointTests {

        @Test
        void shouldInitiateConnectionSuccessfully() throws Exception {
            UUID profileId = UUID.randomUUID();
            BankConnectionRequestDto request = new BankConnectionRequestDto();
            request.setMobileNumber("9876543210");
            request.setProfileId(profileId);

            BankConnectionResponseDto response = new BankConnectionResponseDto();
            response.setConsentId("consent-123");
            response.setRedirectUrl("https://setu.co/consent/consent-123");
            response.setStatus("PENDING");

            when(bankConnectionService.initiateConnection(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/bank/connect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.consentId").value("consent-123"))
                    .andExpect(jsonPath("$.redirectUrl").value("https://setu.co/consent/consent-123"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        void shouldReturnBadRequestOnError() throws Exception {
            BankConnectionRequestDto request = new BankConnectionRequestDto();
            request.setMobileNumber("invalid");
            request.setProfileId(UUID.randomUUID());

            when(bankConnectionService.initiateConnection(any()))
                    .thenThrow(new RuntimeException("Invalid mobile number"));

            mockMvc.perform(post("/api/v1/bank/connect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("FAILED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bank/accounts")
    class GetAccountsEndpointTests {

        @Test
        void shouldReturnAccountsForProfile() throws Exception {
            BankAccountDto account = new BankAccountDto();
            account.setId(UUID.randomUUID());
            account.setConsentStatus("ACTIVE");
            account.setBankName("SBI");

            when(bankConnectionService.getAccountsForProfile(testProfileId)).thenReturn(List.of(account));

            mockMvc.perform(get("/api/v1/bank/accounts")
                            .principal(new UsernamePasswordAuthenticationToken(testPrincipal, null, Collections.emptyList())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].consentStatus").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].bankName").value("SBI"));
        }

        @Test
        void shouldReturnEmptyListForNoAccounts() throws Exception {
            when(bankConnectionService.getAccountsForProfile(testProfileId)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/bank/accounts")
                            .principal(new UsernamePasswordAuthenticationToken(testPrincipal, null, Collections.emptyList())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/bank/accounts/{id}/sync")
    class SyncAccountEndpointTests {

        @Test
        void shouldSyncAccountSuccessfully() throws Exception {
            UUID accountId = UUID.randomUUID();
            BankConnectionResponseDto response = new BankConnectionResponseDto();
            response.setBankAccountId(accountId);
            response.setStatus("SUCCESS");
            response.setMessage("Synced 10 transactions");

            when(bankConnectionService.syncAccount(accountId)).thenReturn(response);

            mockMvc.perform(post("/api/v1/bank/accounts/{id}/sync", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Synced 10 transactions"));
        }

        @Test
        void shouldReturnBadRequestOnSyncError() throws Exception {
            UUID accountId = UUID.randomUUID();
            when(bankConnectionService.syncAccount(accountId))
                    .thenThrow(new RuntimeException("Consent not active"));

            mockMvc.perform(post("/api/v1/bank/accounts/{id}/sync", accountId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("FAILED"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/bank/accounts/{id}")
    class DisconnectAccountEndpointTests {

        @Test
        void shouldDisconnectAccountSuccessfully() throws Exception {
            UUID accountId = UUID.randomUUID();
            BankConnectionResponseDto response = new BankConnectionResponseDto();
            response.setStatus("SUCCESS");
            response.setMessage("Account disconnected");
            when(bankConnectionService.disconnectAccount(any(UUID.class))).thenReturn(response);

            mockMvc.perform(delete("/api/v1/bank/accounts/{id}", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        void shouldReturnBadRequestOnDisconnectError() throws Exception {
            UUID accountId = UUID.randomUUID();
            when(bankConnectionService.disconnectAccount(any(UUID.class)))
                    .thenThrow(new RuntimeException("Account not found"));

            mockMvc.perform(delete("/api/v1/bank/accounts/{id}", accountId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("FAILED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bank/accounts/{id}/consent-status")
    class ConsentStatusEndpointTests {

        @Test
        void shouldReturnConsentStatus() throws Exception {
            UUID accountId = UUID.randomUUID();
            BankConnectionResponseDto response = new BankConnectionResponseDto();
            response.setBankAccountId(accountId);
            response.setStatus("ACTIVE");

            when(bankConnectionService.getConsentStatus(accountId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/bank/accounts/{id}/consent-status", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bank/accounts/{id}/transactions")
    class GetTransactionsEndpointTests {

        @Test
        void shouldReturnTransactions() throws Exception {
            UUID accountId = UUID.randomUUID();
            when(bankConnectionService.getTransactionsForAccount(eq(accountId), any(), any(), anyInt(), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/bank/accounts/{id}/transactions", accountId)
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}