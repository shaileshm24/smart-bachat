package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.config.SetuConfig;
import com.ametsa.smartbachat.entity.BankAccount;
import com.ametsa.smartbachat.repository.BankAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled service for automatic bank account synchronization.
 * Periodically syncs transactions for all active bank accounts.
 */
@Service
public class BankSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(BankSyncScheduler.class);

    private final BankAccountRepository bankAccountRepository;
    private final BankConnectionService bankConnectionService;
    private final SetuConfig setuConfig;

    public BankSyncScheduler(
            BankAccountRepository bankAccountRepository,
            BankConnectionService bankConnectionService,
            SetuConfig setuConfig) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankConnectionService = bankConnectionService;
        this.setuConfig = setuConfig;
    }

    /**
     * Sync all active bank accounts every 6 hours.
     * Only syncs accounts that haven't been synced in the last 4 hours.
     */
    @Scheduled(cron = "${setu.sync-cron:0 0 */6 * * *}")
    public void syncAllActiveAccounts() {
        log.info("[Scheduler] Starting scheduled sync for all active accounts");

        List<BankAccount> activeAccounts = bankAccountRepository.findByConsentStatus("ACTIVE");
        
        if (activeAccounts.isEmpty()) {
            log.info("[Scheduler] No active accounts to sync");
            return;
        }

        Instant fourHoursAgo = Instant.now().minus(4, ChronoUnit.HOURS);
        int syncedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (BankAccount account : activeAccounts) {
            // Skip if recently synced
            if (account.getLastSyncedAt() != null && account.getLastSyncedAt().isAfter(fourHoursAgo)) {
                log.debug("[Scheduler] Skipping account {} - recently synced", account.getId());
                skippedCount++;
                continue;
            }

            try {
                log.info("[Scheduler] Syncing account: {}", account.getId());
                bankConnectionService.syncAccount(account.getId());
                syncedCount++;
            } catch (Exception e) {
                log.error("[Scheduler] Failed to sync account {}: {}", account.getId(), e.getMessage());
                failedCount++;
                
                // Update error message on account
                account.setErrorMessage("Sync failed: " + e.getMessage());
                account.setUpdatedAt(Instant.now());
                bankAccountRepository.save(account);
            }
        }

        log.info("[Scheduler] Sync complete - synced: {}, skipped: {}, failed: {}", 
                syncedCount, skippedCount, failedCount);
    }

    /**
     * Check for expiring consents daily and log warnings.
     * Consents expiring within 7 days will be flagged.
     */
    @Scheduled(cron = "${setu.consent-check-cron:0 0 9 * * *}")
    public void checkExpiringConsents() {
        log.info("[Scheduler] Checking for expiring consents");

        List<BankAccount> activeAccounts = bankAccountRepository.findByConsentStatus("ACTIVE");
        Instant sevenDaysFromNow = Instant.now().plus(7, ChronoUnit.DAYS);

        int expiringCount = 0;
        for (BankAccount account : activeAccounts) {
            if (account.getConsentExpiresAt() != null && 
                account.getConsentExpiresAt().isBefore(sevenDaysFromNow)) {
                log.warn("[Scheduler] Consent expiring soon for account {} (profile: {}), expires: {}",
                        account.getId(), account.getProfileId(), account.getConsentExpiresAt());
                expiringCount++;
                // TODO: Send notification to user about expiring consent
            }
        }

        log.info("[Scheduler] Found {} accounts with expiring consents", expiringCount);
    }

    /**
     * Clean up stale pending consents (older than 24 hours).
     */
    @Scheduled(cron = "${setu.cleanup-cron:0 0 2 * * *}")
    public void cleanupStalePendingConsents() {
        log.info("[Scheduler] Cleaning up stale pending consents");

        List<BankAccount> pendingAccounts = bankAccountRepository.findByConsentStatus("PENDING");
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);

        int cleanedCount = 0;
        for (BankAccount account : pendingAccounts) {
            if (account.getCreatedAt() != null && account.getCreatedAt().isBefore(oneDayAgo)) {
                log.info("[Scheduler] Marking stale consent as EXPIRED: {}", account.getId());
                account.setConsentStatus("EXPIRED");
                account.setUpdatedAt(Instant.now());
                bankAccountRepository.save(account);
                cleanedCount++;
            }
        }

        log.info("[Scheduler] Cleaned up {} stale pending consents", cleanedCount);
    }
}

