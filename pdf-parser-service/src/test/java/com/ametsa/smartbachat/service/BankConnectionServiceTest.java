package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.config.SetuConfig;
import com.ametsa.smartbachat.dto.BankAccountDto;
import com.ametsa.smartbachat.dto.BankConnectionRequestDto;
import com.ametsa.smartbachat.dto.BankConnectionResponseDto;
import com.ametsa.smartbachat.dto.setu.SetuConsentResponse;
import com.ametsa.smartbachat.dto.setu.SetuDataSessionResponse;
import com.ametsa.smartbachat.dto.setu.SetuFIDataResponse;
import com.ametsa.smartbachat.entity.BankAccount;
import com.ametsa.smartbachat.repository.BankAccountRepository;
import com.ametsa.smartbachat.repository.SyncHistoryRepository;
import com.ametsa.smartbachat.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankConnectionServiceTest {

    @Mock private SetuAggregatorService setuService;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SyncHistoryRepository syncHistoryRepository;
    @Mock private BankTransactionMapper transactionMapper;

    private BankConnectionService service;
    private SetuConfig setuConfig;

    @BeforeEach
    void setUp() {
        setuConfig = createSetuConfig();
        service = new BankConnectionService(
                setuService, bankAccountRepository, transactionRepository,
                syncHistoryRepository, transactionMapper, setuConfig);
    }

    @Nested
    @DisplayName("Initiate Connection Tests")
    class InitiateConnectionTests {

        @Test
        void shouldInitiateConnectionSuccessfully() throws Exception {
            UUID profileId = UUID.randomUUID();
            BankConnectionRequestDto request = new BankConnectionRequestDto();
            request.setMobileNumber("9876543210");
            request.setProfileId(profileId);

            SetuConsentResponse consentResponse = new SetuConsentResponse();
            consentResponse.setId("consent-123");
            consentResponse.setUrl("https://setu.co/consent/consent-123");
            consentResponse.setStatus("PENDING");

            when(setuService.createConsent(anyString(), any(LocalDate.class), any(LocalDate.class))).thenReturn(consentResponse);
            when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            BankConnectionResponseDto response = service.initiateConnection(request);

            assertNotNull(response);
            assertEquals("consent-123", response.getConsentId());
            assertEquals("https://setu.co/consent/consent-123", response.getRedirectUrl());
            assertEquals("PENDING", response.getStatus());

            ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
            verify(bankAccountRepository).save(captor.capture());
            assertEquals(profileId, captor.getValue().getProfileId());
            assertEquals("consent-123", captor.getValue().getConsentId());
        }
    }

    @Nested
    @DisplayName("Sync Account Tests")
    class SyncAccountTests {

        @Test
        void shouldSyncAccountSuccessfully() throws Exception {
            UUID accountId = UUID.randomUUID();
            BankAccount account = createBankAccount(accountId, "ACTIVE");

            SetuDataSessionResponse sessionResponse = new SetuDataSessionResponse();
            sessionResponse.setId("session-456");

            SetuFIDataResponse dataResponse = new SetuFIDataResponse();
            dataResponse.setFips(List.of());

            when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(setuService.createDataSession(anyString(), any(), any())).thenReturn(sessionResponse);
            when(setuService.fetchSessionData(anyString())).thenReturn(dataResponse);
            when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));
            when(syncHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BankConnectionResponseDto response = service.syncAccount(accountId);

            assertNotNull(response);
            assertEquals("SUCCESS", response.getStatus());
            verify(bankAccountRepository, times(1)).save(any(BankAccount.class));
        }

        @Test
        void shouldThrowExceptionForInactiveConsent() {
            UUID accountId = UUID.randomUUID();
            BankAccount account = createBankAccount(accountId, "PENDING");

            when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

            assertThrows(RuntimeException.class, () -> service.syncAccount(accountId));
        }

        @Test
        void shouldThrowExceptionForNonExistentAccount() {
            UUID accountId = UUID.randomUUID();
            when(bankAccountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> service.syncAccount(accountId));
        }
    }

    @Nested
    @DisplayName("Get Accounts Tests")
    class GetAccountsTests {

        @Test
        void shouldGetAccountsForProfile() {
            UUID profileId = UUID.randomUUID();
            BankAccount account1 = createBankAccount(UUID.randomUUID(), "ACTIVE");
            BankAccount account2 = createBankAccount(UUID.randomUUID(), "ACTIVE");

            when(bankAccountRepository.findByProfileIdOrderByCreatedAtDesc(profileId)).thenReturn(List.of(account1, account2));

            List<BankAccountDto> accounts = service.getAccountsForProfile(profileId);

            assertEquals(2, accounts.size());
        }

        @Test
        void shouldReturnEmptyListForNoAccounts() {
            UUID profileId = UUID.randomUUID();
            when(bankAccountRepository.findByProfileIdOrderByCreatedAtDesc(profileId)).thenReturn(List.of());

            List<BankAccountDto> accounts = service.getAccountsForProfile(profileId);

            assertTrue(accounts.isEmpty());
        }
    }

    @Nested
    @DisplayName("Disconnect Account Tests")
    class DisconnectAccountTests {

        @Test
        void shouldDisconnectAccountSuccessfully() throws Exception {
            UUID accountId = UUID.randomUUID();
            BankAccount account = createBankAccount(accountId, "ACTIVE");

            when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
            doNothing().when(setuService).revokeConsent(anyString());
            when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            service.disconnectAccount(accountId);

            ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
            verify(bankAccountRepository).save(captor.capture());
            assertEquals("REVOKED", captor.getValue().getConsentStatus());
            assertFalse(captor.getValue().getIsActive());
        }
    }

    private BankAccount createBankAccount(UUID id, String consentStatus) {
        BankAccount account = new BankAccount();
        account.setId(id);
        account.setProfileId(UUID.randomUUID());
        account.setConsentId("consent-" + id);
        account.setConsentStatus(consentStatus);
        account.setIsActive(true);
        account.setCreatedAt(Instant.now());
        return account;
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
        config.setVuaSuffix("@setu-aa");
        return config;
    }
}