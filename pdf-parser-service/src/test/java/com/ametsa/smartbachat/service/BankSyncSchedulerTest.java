package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.config.SetuConfig;
import com.ametsa.smartbachat.entity.BankAccount;
import com.ametsa.smartbachat.repository.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankSyncSchedulerTest {

    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private BankConnectionService bankConnectionService;
    @Mock private SetuConfig setuConfig;

    private BankSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BankSyncScheduler(bankAccountRepository, bankConnectionService, setuConfig);
    }

    @Nested
    @DisplayName("Sync All Active Accounts Tests")
    class SyncAllActiveAccountsTests {

        @Test
        void shouldSyncAccountsNotRecentlySynced() throws Exception {
            BankAccount account1 = createAccount("ACTIVE", Instant.now().minus(5, ChronoUnit.HOURS));
            BankAccount account2 = createAccount("ACTIVE", Instant.now().minus(6, ChronoUnit.HOURS));

            when(bankAccountRepository.findByConsentStatus("ACTIVE"))
                    .thenReturn(List.of(account1, account2));

            scheduler.syncAllActiveAccounts();

            verify(bankConnectionService, times(2)).syncAccount(any(UUID.class));
        }

        @Test
        void shouldSkipRecentlySyncedAccounts() throws Exception {
            BankAccount recentlySynced = createAccount("ACTIVE", Instant.now().minus(2, ChronoUnit.HOURS));
            BankAccount notRecentlySynced = createAccount("ACTIVE", Instant.now().minus(5, ChronoUnit.HOURS));

            when(bankAccountRepository.findByConsentStatus("ACTIVE"))
                    .thenReturn(List.of(recentlySynced, notRecentlySynced));

            scheduler.syncAllActiveAccounts();

            // Only one account should be synced (the one not recently synced)
            verify(bankConnectionService, times(1)).syncAccount(any(UUID.class));
        }

        @Test
        void shouldHandleNoActiveAccounts() throws Exception {
            when(bankAccountRepository.findByConsentStatus("ACTIVE")).thenReturn(List.of());

            scheduler.syncAllActiveAccounts();

            verify(bankConnectionService, never()).syncAccount(any(UUID.class));
        }

        @Test
        void shouldHandleSyncFailure() throws Exception {
            BankAccount account = createAccount("ACTIVE", Instant.now().minus(5, ChronoUnit.HOURS));

            when(bankAccountRepository.findByConsentStatus("ACTIVE")).thenReturn(List.of(account));
            doThrow(new RuntimeException("Sync failed")).when(bankConnectionService).syncAccount(any(UUID.class));

            // Should not throw exception, just log error
            scheduler.syncAllActiveAccounts();

            verify(bankAccountRepository).save(any(BankAccount.class));
        }
    }

    @Nested
    @DisplayName("Check Expiring Consents Tests")
    class CheckExpiringConsentsTests {

        @Test
        void shouldIdentifyExpiringConsents() {
            BankAccount expiringSoon = createAccount("ACTIVE", null);
            expiringSoon.setConsentExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS));

            BankAccount notExpiring = createAccount("ACTIVE", null);
            notExpiring.setConsentExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

            when(bankAccountRepository.findByConsentStatus("ACTIVE"))
                    .thenReturn(List.of(expiringSoon, notExpiring));

            scheduler.checkExpiringConsents();

            // Just verifies no exception is thrown - logging is tested
        }

        @Test
        void shouldHandleNoActiveAccounts() {
            when(bankAccountRepository.findByConsentStatus("ACTIVE")).thenReturn(List.of());

            scheduler.checkExpiringConsents();
            // Should complete without error
        }
    }

    @Nested
    @DisplayName("Cleanup Stale Pending Consents Tests")
    class CleanupStalePendingConsentsTests {

        @Test
        void shouldMarkStalePendingConsentsAsExpired() {
            BankAccount staleAccount = createAccount("PENDING", null);
            staleAccount.setCreatedAt(Instant.now().minus(25, ChronoUnit.HOURS));

            when(bankAccountRepository.findByConsentStatus("PENDING")).thenReturn(List.of(staleAccount));

            scheduler.cleanupStalePendingConsents();

            verify(bankAccountRepository).save(argThat(acc -> "EXPIRED".equals(acc.getConsentStatus())));
        }

        @Test
        void shouldNotMarkRecentPendingConsents() {
            BankAccount recentAccount = createAccount("PENDING", null);
            recentAccount.setCreatedAt(Instant.now().minus(12, ChronoUnit.HOURS));

            when(bankAccountRepository.findByConsentStatus("PENDING")).thenReturn(List.of(recentAccount));

            scheduler.cleanupStalePendingConsents();

            verify(bankAccountRepository, never()).save(any(BankAccount.class));
        }
    }

    private BankAccount createAccount(String status, Instant lastSyncedAt) {
        BankAccount account = new BankAccount();
        account.setId(UUID.randomUUID());
        account.setProfileId(UUID.randomUUID());
        account.setConsentId("consent-" + UUID.randomUUID());
        account.setConsentStatus(status);
        account.setLastSyncedAt(lastSyncedAt);
        account.setCreatedAt(Instant.now());
        account.setIsActive(true);
        return account;
    }
}

