package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.config.SetuConfig;
import com.ametsa.smartbachat.dto.BankAccountDto;
import com.ametsa.smartbachat.dto.BankConnectionRequestDto;
import com.ametsa.smartbachat.dto.BankConnectionResponseDto;
import com.ametsa.smartbachat.dto.setu.*;
import com.ametsa.smartbachat.entity.AccountHolderEntity;
import com.ametsa.smartbachat.entity.BankAccount;
import com.ametsa.smartbachat.entity.SyncHistory;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.repository.AccountHolderRepository;
import com.ametsa.smartbachat.repository.BankAccountRepository;
import com.ametsa.smartbachat.repository.SyncHistoryRepository;
import com.ametsa.smartbachat.repository.TransactionRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing bank account connections via Account Aggregator.
 */
@Service
public class BankConnectionService {

    private static final Logger log = LoggerFactory.getLogger(BankConnectionService.class);

    private final SetuAggregatorService setuService;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final AccountHolderRepository accountHolderRepository;
    private final BankTransactionMapper transactionMapper;
    private final SetuConfig setuConfig;
    private final SecurityUtils securityUtils;

    public BankConnectionService(
            SetuAggregatorService setuService,
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            SyncHistoryRepository syncHistoryRepository,
            AccountHolderRepository accountHolderRepository,
            BankTransactionMapper transactionMapper,
            SetuConfig setuConfig,
            SecurityUtils securityUtils) {
        this.setuService = setuService;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.syncHistoryRepository = syncHistoryRepository;
        this.accountHolderRepository = accountHolderRepository;
        this.transactionMapper = transactionMapper;
        this.setuConfig = setuConfig;
        this.securityUtils = securityUtils;
    }

    /**
     * Initiate bank account connection.
     */
    @Transactional
    public BankConnectionResponseDto initiateConnection(BankConnectionRequestDto request) throws Exception {
        UUID userId = securityUtils.requireCurrentUserId();
        log.info("Initiating bank connection for user: {}, profile: {}", userId, request.getProfileId());

        // Calculate data range
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusMonths(setuConfig.getDataFetchMonths());

        if (request.getDataFromDate() != null) {
            fromDate = LocalDate.parse(request.getDataFromDate());
        }
        if (request.getDataToDate() != null) {
            toDate = LocalDate.parse(request.getDataToDate());
        }

        // Create consent with Setu
        SetuConsentResponse consentResponse = setuService.createConsent(
                request.getMobileNumber(), fromDate, toDate);

        // Create bank account record (pending consent)
        BankAccount bankAccount = new BankAccount();
        bankAccount.setId(UUID.randomUUID());
        bankAccount.setUserId(userId);
        bankAccount.setProfileId(request.getProfileId());
        bankAccount.setConsentId(consentResponse.getId());
        bankAccount.setConsentStatus("PENDING");
        bankAccount.setAggregator("SETU");
        bankAccount.setIsActive(false);
        bankAccount.setCreatedAt(Instant.now());
        bankAccount.setUpdatedAt(Instant.now());

        bankAccountRepository.save(bankAccount);

        log.info("Created bank account {} with consent {}", bankAccount.getId(), consentResponse.getId());

        return BankConnectionResponseDto.builder()
                .bankAccountId(bankAccount.getId())
                .consentId(consentResponse.getId())
                .redirectUrl(consentResponse.getUrl())
                .status("PENDING")
                .build();
    }

    /**
     * Get all bank accounts for a profile.
     */
    public List<BankAccountDto> getAccountsForProfile(UUID profileId) {
        return bankAccountRepository.findByProfileIdOrderByCreatedAtDesc(profileId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific bank account.
     */
    public BankAccountDto getAccount(UUID accountId) {
        return bankAccountRepository.findById(accountId)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Sync transactions for a bank account.
     */
    @Transactional
    public BankConnectionResponseDto syncAccount(UUID accountId) throws Exception {
        return syncAccount(accountId, "MANUAL");
    }

    /**
     * Sync transactions for a bank account with trigger type.
     */
    @Transactional
    public BankConnectionResponseDto syncAccount(UUID accountId, String triggerType) throws Exception {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found: " + accountId));

        if (!"ACTIVE".equals(account.getConsentStatus())) {
            throw new RuntimeException("Consent is not active for this account");
        }

        log.info("Syncing account: {} (trigger: {})", accountId, triggerType);

        // Create sync history record
        SyncHistory syncHistory = new SyncHistory();
        syncHistory.setUserId(account.getUserId());
        syncHistory.setBankAccountId(accountId);
        syncHistory.setProfileId(account.getProfileId());
        syncHistory.setTriggerType(triggerType);
        syncHistory.setStatus("IN_PROGRESS");
        syncHistoryRepository.save(syncHistory);

        try {
            // Get consent details to find the allowed data range
            SetuConsentResponse consentDetails = setuService.getConsentStatus(account.getConsentId(), true);
            log.info("Consent details - status: {}, detail: {}",
                    consentDetails.getStatus(),
                    consentDetails.getDetail() != null ? "present" : "null");

            LocalDate fromDate;
            LocalDate toDate;

            // Use the consent's data range - the data session range must be within this
            // For toDate, use yesterday to avoid any timezone issues with "today"
            if (consentDetails.getDetail() != null && consentDetails.getDetail().getDataRange() != null) {
                SetuConsentResponse.DataRange consentDataRange = consentDetails.getDetail().getDataRange();
                log.info("Consent dataRange - from: {}, to: {}", consentDataRange.getFrom(), consentDataRange.getTo());
                fromDate = LocalDate.parse(consentDataRange.getFrom().substring(0, 10)); // Extract date part
                LocalDate consentToDate = LocalDate.parse(consentDataRange.getTo().substring(0, 10));
                // Use the earlier of: consent's to date or yesterday (to avoid timezone issues)
                LocalDate yesterday = LocalDate.now().minusDays(1);
                toDate = consentToDate.isBefore(yesterday) ? consentToDate : yesterday;
                log.info("Using consent data range: {} to {} (consent to: {})", fromDate, toDate, consentToDate);
            } else {
                // Fallback to default range
                toDate = LocalDate.now().minusDays(1); // Use yesterday
                fromDate = account.getLastSyncedAt() != null
                        ? LocalDate.ofInstant(account.getLastSyncedAt(), java.time.ZoneId.systemDefault())
                        : toDate.minusMonths(setuConfig.getDataFetchMonths());
                log.warn("Consent data range not available, using default: {} to {}", fromDate, toDate);
            }

            syncHistory.setDataFromDate(fromDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            syncHistory.setDataToDate(toDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

            SetuDataSessionResponse sessionResponse = setuService.createDataSession(
                    account.getConsentId(), fromDate, toDate);

            syncHistory.markInProgress(sessionResponse.getId());
            syncHistoryRepository.save(syncHistory);

            // Fetch data (in production, this would be async via webhook)
            SetuFIDataResponse dataResponse = setuService.fetchSessionData(sessionResponse.getId());

            // Debug logging
            log.info("Data session response - status: {}, fips count: {}",
                    dataResponse.getStatus(),
                    dataResponse.getFips() != null ? dataResponse.getFips().size() : 0);

            if (dataResponse.getFips() != null) {
                for (SetuFIDataResponse.FIPData fip : dataResponse.getFips()) {
                    log.info("FIP: {}, accounts count: {}",
                            fip.getFipId(),
                            fip.getAccounts() != null ? fip.getAccounts().size() : 0);
                    if (fip.getAccounts() != null) {
                        for (SetuFIDataResponse.AccountData acc : fip.getAccounts()) {
                            log.info("Account: {}, fiType: {}, has data: {}",
                                    acc.getMaskedAccNumber(),
                                    acc.getFiType(),
                                    acc.getData() != null);
                            if (acc.getData() != null) {
                                boolean hasAccountInfo = acc.getData().getAccount() != null;
                                boolean hasAccountTxns = hasAccountInfo && acc.getData().getAccount().getTransactions() != null;
                                boolean hasDataTxns = acc.getData().getTransactions() != null;
                                log.info("  Has account info: {}, has account.transactions: {}, has data.transactions: {}",
                                        hasAccountInfo, hasAccountTxns, hasDataTxns);
                                if (hasAccountTxns) {
                                    log.info("  Transaction list (from account): {}",
                                            acc.getData().getAccount().getTransactions().getTransaction() != null
                                                    ? acc.getData().getAccount().getTransactions().getTransaction().size()
                                                    : "null");
                                } else if (hasDataTxns) {
                                    log.info("  Transaction list (from data): {}",
                                            acc.getData().getTransactions().getTransaction() != null
                                                    ? acc.getData().getTransactions().getTransaction().size()
                                                    : "null");
                                }
                            }
                        }
                    }
                }
            }

            // Process and save transactions
            SyncResult result = processAndSaveTransactionsWithCount(dataResponse, account);

            // Update sync history
            syncHistory.markSuccess(result.fetched, result.saved, result.skipped);
            syncHistoryRepository.save(syncHistory);

            // Update account
            account.setLastSessionId(sessionResponse.getId());
            account.setLastSyncedAt(Instant.now());
            account.setUpdatedAt(Instant.now());
            account.setErrorMessage(null);
            bankAccountRepository.save(account);

            BankConnectionResponseDto response = new BankConnectionResponseDto();
            response.setBankAccountId(accountId);
            response.setStatus("SUCCESS");
            response.setMessage("Synced " + result.saved + " transactions (" + result.skipped + " duplicates skipped)");
            return response;

        } catch (Exception e) {
            // Update sync history with error
            syncHistory.markFailed("SYNC_ERROR", e.getMessage());
            syncHistoryRepository.save(syncHistory);

            // Update account error
            account.setErrorMessage("Sync failed: " + e.getMessage());
            account.setUpdatedAt(Instant.now());
            bankAccountRepository.save(account);

            throw e;
        }
    }

    // Helper class for sync results
    private static class SyncResult {
        int fetched;
        int saved;
        int skipped;
    }

    /**
     * Disconnect a bank account (revoke consent).
     */
    @Transactional
    public BankConnectionResponseDto disconnectAccount(UUID accountId) throws Exception {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found: " + accountId));

        if (account.getConsentId() != null && "ACTIVE".equals(account.getConsentStatus())) {
            setuService.revokeConsent(account.getConsentId());
        }

        account.setConsentStatus("REVOKED");
        account.setIsActive(false);
        account.setUpdatedAt(Instant.now());
        bankAccountRepository.save(account);

        BankConnectionResponseDto response = new BankConnectionResponseDto();
        response.setBankAccountId(accountId);
        response.setStatus("REVOKED");
        response.setMessage("Bank account disconnected successfully");
        return response;
    }

    /**
     * Get consent status for a bank account.
     */
    public BankConnectionResponseDto getConsentStatus(UUID accountId) throws Exception {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found: " + accountId));

        // Optionally refresh from Setu
        if (account.getConsentId() != null && "PENDING".equals(account.getConsentStatus())) {
            SetuConsentResponse consentResponse = setuService.getConsentStatus(account.getConsentId());
            if (!account.getConsentStatus().equals(consentResponse.getStatus())) {
                account.setConsentStatus(consentResponse.getStatus());
                account.setUpdatedAt(Instant.now());
                if ("ACTIVE".equals(consentResponse.getStatus())) {
                    account.setIsActive(true);
                    account.setConsentApprovedAt(Instant.now());
                }
                bankAccountRepository.save(account);
            }
        }

        BankConnectionResponseDto response = new BankConnectionResponseDto();
        response.setBankAccountId(accountId);
        response.setConsentId(account.getConsentId());
        response.setStatus(account.getConsentStatus());
        return response;
    }

    /**
     * Update account nickname.
     */
    @Transactional
    public BankAccountDto updateNickname(UUID accountId, String nickname) {
        return bankAccountRepository.findById(accountId)
                .map(account -> {
                    account.setNickname(nickname);
                    account.setUpdatedAt(Instant.now());
                    bankAccountRepository.save(account);
                    return toDto(account);
                })
                .orElse(null);
    }

    /**
     * Set account as primary for a profile.
     */
    @Transactional
    public BankAccountDto setPrimaryAccount(UUID accountId, UUID profileId) {
        // Unset current primary
        bankAccountRepository.findByProfileIdAndIsActiveTrue(profileId)
                .forEach(acc -> {
                    acc.setIsPrimary(false);
                    bankAccountRepository.save(acc);
                });

        // Set new primary
        return bankAccountRepository.findById(accountId)
                .map(account -> {
                    account.setIsPrimary(true);
                    account.setUpdatedAt(Instant.now());
                    bankAccountRepository.save(account);
                    return toDto(account);
                })
                .orElse(null);
    }

    /**
     * Process webhook notification from Setu.
     */
    @Transactional
    public void handleWebhook(SetuWebhookPayload payload) {
        log.info("Received webhook: type={}, consentId={}, status={}",
                payload.getType(), payload.getConsentId(), payload.getStatus());

        if ("CONSENT_STATUS_UPDATE".equals(payload.getType())) {
            handleConsentStatusUpdate(payload);
        } else if ("SESSION_STATUS_UPDATE".equals(payload.getType())) {
            handleSessionStatusUpdate(payload);
        }
    }

    private void handleConsentStatusUpdate(SetuWebhookPayload payload) {
        bankAccountRepository.findByConsentId(payload.getConsentId())
                .ifPresent(existingAccount -> {
                    log.info("Processing consent status update for consentId: {}, status: {}, accounts: {}",
                            payload.getConsentId(), payload.getStatus(),
                            payload.getAccounts() != null ? payload.getAccounts().size() : 0);

                    if ("ACTIVE".equals(payload.getStatus()) &&
                        payload.getAccounts() != null && !payload.getAccounts().isEmpty()) {

                        // Handle multiple accounts from single consent
                        // First account updates the existing record, additional accounts create new records
                        boolean isFirstAccount = true;

                        for (SetuWebhookPayload.LinkedAccount linked : payload.getAccounts()) {
                            log.info("Processing linked account: fipId={}, maskedAccNumber={}, fiType={}",
                                    linked.getFipId(), linked.getMaskedAccNumber(), linked.getFiType());

                            if (isFirstAccount) {
                                // Update the existing account record with first linked account
                                existingAccount.setConsentStatus(payload.getStatus());
                                existingAccount.setIsActive(true);
                                existingAccount.setConsentApprovedAt(Instant.now());
                                existingAccount.setFipId(linked.getFipId());
                                existingAccount.setMaskedAccountNumber(linked.getMaskedAccNumber());
                                existingAccount.setFiType(linked.getFiType());
                                existingAccount.setLinkedAccountRef(linked.getLinkRefNumber());
                                existingAccount.setBankName(extractBankName(linked.getFipId()));
                                existingAccount.setUpdatedAt(Instant.now());
                                bankAccountRepository.save(existingAccount);
                                log.info("Updated existing account {} with first linked account: {}",
                                        existingAccount.getId(), linked.getMaskedAccNumber());
                                isFirstAccount = false;
                            } else {
                                // Create new account record for additional linked accounts
                                // Check if this account already exists (by masked account number)
                                if (!bankAccountRepository.existsByProfileIdAndMaskedAccountNumber(
                                        existingAccount.getProfileId(), linked.getMaskedAccNumber())) {

                                    BankAccount newAccount = new BankAccount();
                                    newAccount.setId(UUID.randomUUID());
                                    newAccount.setUserId(existingAccount.getUserId());
                                    newAccount.setProfileId(existingAccount.getProfileId());
                                    newAccount.setConsentId(payload.getConsentId()); // Same consent
                                    newAccount.setConsentStatus("ACTIVE");
                                    newAccount.setIsActive(true);
                                    newAccount.setConsentApprovedAt(Instant.now());
                                    newAccount.setAggregator("SETU");
                                    newAccount.setFipId(linked.getFipId());
                                    newAccount.setMaskedAccountNumber(linked.getMaskedAccNumber());
                                    newAccount.setFiType(linked.getFiType());
                                    newAccount.setLinkedAccountRef(linked.getLinkRefNumber());
                                    newAccount.setBankName(extractBankName(linked.getFipId()));
                                    newAccount.setCreatedAt(Instant.now());
                                    newAccount.setUpdatedAt(Instant.now());
                                    bankAccountRepository.save(newAccount);
                                    log.info("Created new account {} for additional linked account: {}",
                                            newAccount.getId(), linked.getMaskedAccNumber());
                                } else {
                                    log.info("Account with masked number {} already exists, skipping",
                                            linked.getMaskedAccNumber());
                                }
                            }
                        }
                    } else {
                        // Non-ACTIVE status or no accounts - just update status
                        existingAccount.setConsentStatus(payload.getStatus());
                        existingAccount.setUpdatedAt(Instant.now());

                        if ("REVOKED".equals(payload.getStatus()) ||
                            "EXPIRED".equals(payload.getStatus()) ||
                            "REJECTED".equals(payload.getStatus())) {
                            existingAccount.setIsActive(false);
                        }

                        bankAccountRepository.save(existingAccount);
                        log.info("Updated consent status to {} for account: {}",
                                payload.getStatus(), existingAccount.getId());
                    }
                });
    }

    private void handleSessionStatusUpdate(SetuWebhookPayload payload) {
        log.info("Session status update: sessionId={}, status={}",
                payload.getSessionId(), payload.getStatus());

        // If session is completed, fetch the data
        if ("COMPLETED".equalsIgnoreCase(payload.getStatus()) ||
            "ACTIVE".equalsIgnoreCase(payload.getStatus())) {

            // Find the account by session ID
            String sessionId = payload.getSessionId();
            if (sessionId == null) {
                log.warn("Session status update without sessionId");
                return;
            }

            // Find account with this session ID
            bankAccountRepository.findAll().stream()
                    .filter(acc -> sessionId.equals(acc.getLastSessionId()))
                    .findFirst()
                    .ifPresent(account -> {
                        try {
                            log.info("Auto-fetching data for completed session: {}", sessionId);
                            SetuFIDataResponse dataResponse = setuService.fetchSessionData(sessionId);
                            int savedCount = processAndSaveTransactions(dataResponse, account);

                            account.setLastSyncedAt(Instant.now());
                            account.setUpdatedAt(Instant.now());
                            account.setErrorMessage(null);
                            bankAccountRepository.save(account);

                            log.info("Auto-fetched {} transactions for account {}", savedCount, account.getId());
                        } catch (Exception e) {
                            log.error("Failed to auto-fetch data for session {}: {}", sessionId, e.getMessage());
                            account.setErrorMessage("Auto-fetch failed: " + e.getMessage());
                            account.setUpdatedAt(Instant.now());
                            bankAccountRepository.save(account);
                        }
                    });
        }
    }

    /**
     * Process FI data response and save transactions.
     */
    private int processAndSaveTransactions(SetuFIDataResponse dataResponse, BankAccount account) {
        SyncResult result = processAndSaveTransactionsWithCount(dataResponse, account);
        return result.saved;
    }

    /**
     * Process FI data response and save transactions with detailed counts.
     * IMPORTANT: Setu returns data for ALL accounts linked to a consent.
     * We must match each account in the response to the correct BankAccount record.
     */
    private SyncResult processAndSaveTransactionsWithCount(SetuFIDataResponse dataResponse, BankAccount triggerAccount) {
        SyncResult result = new SyncResult();

        if (dataResponse.getFips() == null) {
            return result;
        }

        // Get all accounts linked to this consent (for multi-account matching)
        List<BankAccount> linkedAccounts = bankAccountRepository.findByConsentIdAndIsActiveTrue(
                triggerAccount.getConsentId());

        log.info("[Setu Sync] Found {} linked accounts for consent: {}",
                linkedAccounts.size(), triggerAccount.getConsentId());

        for (SetuFIDataResponse.FIPData fip : dataResponse.getFips()) {
            if (fip.getAccounts() == null) continue;

            for (SetuFIDataResponse.AccountData accData : fip.getAccounts()) {
                String maskedAccNumber = accData.getMaskedAccNumber();
                log.info("[Setu Sync] Processing account data - maskedAccNumber: {}, fiType: {}, fiStatus: {}",
                        maskedAccNumber, accData.getFiType(), accData.getFiStatus());

                // Find the correct BankAccount for this Setu account
                // If no match found and this is a different account, creates a new BankAccount (webhook fallback)
                BankAccount targetAccount = findMatchingBankAccount(
                        linkedAccounts, maskedAccNumber, triggerAccount, fip, accData);

                if (targetAccount == null) {
                    log.warn("[Setu Sync] No matching BankAccount found for maskedAccNumber: {}, skipping",
                            maskedAccNumber);
                    continue;
                }

                log.info("[Setu Sync] Matched to BankAccount: {} ({})",
                        targetAccount.getId(), targetAccount.getMaskedAccountNumber());

                // Update bank name from FIP ID
                if (targetAccount.getBankName() == null && fip.getFipId() != null) {
                    targetAccount.setBankName(extractBankName(fip.getFipId()));
                }

                // Update account details
                if (accData.getData() != null && accData.getData().getAccount() != null) {
                    SetuFIDataResponse.AccountInfo accInfo = accData.getData().getAccount();

                    log.info("[Setu Sync] Account info - type: {}, branch: {}, ifsc: {}, currentBalance: {}, currency: {}",
                            accInfo.getType(), accInfo.getBranch(), accInfo.getIfsc(),
                            accInfo.getCurrentBalance(), accInfo.getCurrency());

                    // Also check summary for balance (some banks put it there)
                    if (accInfo.getSummary() != null) {
                        log.info("[Setu Sync] Account summary - currentBalance: {}, currency: {}, type: {}",
                                accInfo.getSummary().getCurrentBalance(),
                                accInfo.getSummary().getCurrency(),
                                accInfo.getSummary().getType());
                    }

                    targetAccount.setAccountType(accInfo.getType());
                    targetAccount.setBranch(accInfo.getBranch());
                    targetAccount.setIfsc(accInfo.getIfsc());
                    targetAccount.setCurrency(accInfo.getCurrency());

                    // Try to get balance from account info first, then from summary
                    String balanceStr = accInfo.getCurrentBalance();
                    if (balanceStr == null && accInfo.getSummary() != null) {
                        balanceStr = accInfo.getSummary().getCurrentBalance();
                        log.info("[Setu Sync] Using balance from summary: {}", balanceStr);
                    }

                    if (balanceStr != null) {
                        Long balancePaisa = parseAmountToPaisa(balanceStr);
                        targetAccount.setCurrentBalance(balancePaisa);
                        log.info("[Setu Sync] Set account {} balance: {} paisa (₹{})",
                                targetAccount.getId(), balancePaisa, balancePaisa / 100.0);
                    } else {
                        log.warn("[Setu Sync] No balance found in Setu response for account: {}",
                                targetAccount.getMaskedAccountNumber());
                    }

                    if (accInfo.getHolder() != null) {
                        targetAccount.setAccountHolderName(accInfo.getHolder().getName());
                        log.info("[Setu Sync] Account holder: {}", accInfo.getHolder().getName());
                    }

                    // Process and save account holder profile data
                    processAccountHolders(accInfo, targetAccount);

                    // Save updated account details
                    targetAccount.setUpdatedAt(Instant.now());
                    bankAccountRepository.save(targetAccount);
                } else {
                    log.warn("[Setu Sync] No account data found in response for account: {}",
                            targetAccount.getMaskedAccountNumber());
                }

                // Process transactions - they are inside account.transactions (not data.transactions)
                SetuFIDataResponse.TransactionList txnList = null;
                if (accData.getData() != null && accData.getData().getAccount() != null) {
                    txnList = accData.getData().getAccount().getTransactions();
                }
                // Fallback to data.transactions for backward compatibility
                if (txnList == null && accData.getData() != null) {
                    txnList = accData.getData().getTransactions();
                }

                if (txnList != null && txnList.getTransaction() != null) {
                    List<Transaction> transactions = txnList.getTransaction();
                    result.fetched += transactions.size();

                    log.info("[Setu Sync] Processing {} transactions for account: {} ({})",
                            transactions.size(), targetAccount.getId(), targetAccount.getMaskedAccountNumber());

                    for (Transaction txn : transactions) {
                        // Map transaction with the CORRECT bankAccountId
                        TransactionEntity entity = transactionMapper.mapFromAA(
                                txn, targetAccount.getId(), targetAccount.getProfileId());
                        entity.setUserId(targetAccount.getUserId());

                        // Check for duplicates using dedupeKey (most reliable)
                        if (entity.getDedupeKey() != null &&
                            transactionRepository.existsByDedupeKey(entity.getDedupeKey())) {
                            result.skipped++;
                            continue;
                        }

                        // Fallback: also check by bankTxnId if dedupeKey check passed
                        // (handles edge case where dedupeKey format changed)
                        if (txn.getTxnId() != null &&
                            transactionRepository.existsByBankAccountIdAndBankTxnId(
                                    targetAccount.getId(), txn.getTxnId())) {
                            result.skipped++;
                            continue;
                        }

                        transactionRepository.save(entity);
                        result.saved++;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Find the matching BankAccount for a Setu account response.
     * Matches by masked account number. If no match found and this is a different account
     * than the trigger account, creates a new BankAccount record (fallback for when webhook didn't work).
     */
    private BankAccount findMatchingBankAccount(List<BankAccount> linkedAccounts,
                                                 String maskedAccNumber,
                                                 BankAccount triggerAccount,
                                                 SetuFIDataResponse.FIPData fip,
                                                 SetuFIDataResponse.AccountData accData) {
        if (maskedAccNumber == null) {
            return triggerAccount;
        }

        // Try exact match first
        for (BankAccount acc : linkedAccounts) {
            if (maskedAccNumber.equals(acc.getMaskedAccountNumber())) {
                log.info("[Setu Sync] Exact match found: {} -> BankAccount {}",
                        maskedAccNumber, acc.getId());
                return acc;
            }
        }

        // Try partial match (last 4 digits) - Setu might format differently
        String last4 = maskedAccNumber.length() >= 4
                ? maskedAccNumber.substring(maskedAccNumber.length() - 4)
                : maskedAccNumber;
        for (BankAccount acc : linkedAccounts) {
            if (acc.getMaskedAccountNumber() != null &&
                acc.getMaskedAccountNumber().endsWith(last4)) {
                log.info("[Setu Sync] Partial match found: {} matches {} -> BankAccount {}",
                        maskedAccNumber, acc.getMaskedAccountNumber(), acc.getId());
                return acc;
            }
        }

        // No match found - check if trigger account has no masked number yet (first sync)
        if (triggerAccount.getMaskedAccountNumber() == null ||
            triggerAccount.getMaskedAccountNumber().isEmpty()) {
            // Update trigger account with this masked number
            log.info("[Setu Sync] Trigger account has no masked number, updating with: {}",
                    maskedAccNumber);
            triggerAccount.setMaskedAccountNumber(maskedAccNumber);
            return triggerAccount;
        }

        // Check if this is the same account as trigger (different format)
        String triggerLast4 = triggerAccount.getMaskedAccountNumber().length() >= 4
                ? triggerAccount.getMaskedAccountNumber().substring(
                        triggerAccount.getMaskedAccountNumber().length() - 4)
                : triggerAccount.getMaskedAccountNumber();
        if (last4.equals(triggerLast4)) {
            log.info("[Setu Sync] Masked number {} matches trigger account (last 4 digits)",
                    maskedAccNumber);
            return triggerAccount;
        }

        // This is a DIFFERENT account that doesn't exist in our DB
        // This happens when webhook didn't work (common in local development)
        // Create a new BankAccount record for this account
        log.warn("[Setu Sync] No matching BankAccount for {}. Creating new account (webhook fallback).",
                maskedAccNumber);

        BankAccount newAccount = createMissingBankAccount(triggerAccount, maskedAccNumber, fip, accData);
        linkedAccounts.add(newAccount); // Add to list so subsequent lookups find it
        return newAccount;
    }

    /**
     * Create a missing BankAccount record when Setu returns an account that doesn't exist in our DB.
     * This is a fallback for when the webhook didn't work (common in local development).
     */
    private BankAccount createMissingBankAccount(BankAccount templateAccount,
                                                   String maskedAccNumber,
                                                   SetuFIDataResponse.FIPData fip,
                                                   SetuFIDataResponse.AccountData accData) {
        BankAccount newAccount = new BankAccount();
        newAccount.setId(UUID.randomUUID());
        newAccount.setUserId(templateAccount.getUserId());
        newAccount.setProfileId(templateAccount.getProfileId());
        newAccount.setConsentId(templateAccount.getConsentId()); // Same consent
        newAccount.setConsentStatus("ACTIVE");
        newAccount.setIsActive(true);
        newAccount.setConsentApprovedAt(Instant.now());
        newAccount.setAggregator("SETU");
        newAccount.setMaskedAccountNumber(maskedAccNumber);
        newAccount.setFiType(accData.getFiType());
        newAccount.setLinkedAccountRef(accData.getLinkRefNumber());
        newAccount.setCreatedAt(Instant.now());
        newAccount.setUpdatedAt(Instant.now());

        // Set FIP info
        if (fip != null && fip.getFipId() != null) {
            newAccount.setFipId(fip.getFipId());
            newAccount.setBankName(extractBankName(fip.getFipId()));
        }

        // Save the new account
        bankAccountRepository.save(newAccount);
        log.info("[Setu Sync] Created new BankAccount {} for masked number: {} (webhook fallback)",
                newAccount.getId(), maskedAccNumber);

        return newAccount;
    }

    /**
     * Process and save account holder profile data from Setu response.
     */
    private void processAccountHolders(SetuFIDataResponse.AccountInfo accInfo, BankAccount account) {
        if (accInfo.getProfile() == null || accInfo.getProfile().getHolders() == null) {
            return;
        }

        SetuFIDataResponse.HoldersInfo holdersInfo = accInfo.getProfile().getHolders();
        String holderType = holdersInfo.getType(); // SINGLE or JOINT

        if (holdersInfo.getHolder() == null) {
            return;
        }

        // Delete existing holders for this account (to handle updates)
        accountHolderRepository.deleteByBankAccountId(account.getId());

        for (SetuFIDataResponse.HolderDetail holder : holdersInfo.getHolder()) {
            AccountHolderEntity holderEntity = new AccountHolderEntity();
            holderEntity.setUserId(account.getUserId());
            holderEntity.setBankAccountId(account.getId());
            holderEntity.setHolderType(holderType);
            holderEntity.setName(holder.getName());
            holderEntity.setMobile(holder.getMobile());
            holderEntity.setEmail(holder.getEmail());
            holderEntity.setPan(holder.getPan());
            holderEntity.setAddress(holder.getAddress());
            holderEntity.setNominee(holder.getNominee());
            holderEntity.setCkycCompliance(holder.getCkycCompliance());

            // Parse DOB if present
            if (holder.getDob() != null && !holder.getDob().isEmpty()) {
                try {
                    holderEntity.setDob(LocalDate.parse(holder.getDob()));
                } catch (Exception e) {
                    log.warn("Could not parse DOB: {}", holder.getDob());
                }
            }

            accountHolderRepository.save(holderEntity);
            log.debug("Saved account holder: {} for account: {}", holder.getName(), account.getId());
        }
    }

    /**
     * Extract bank name from FIP ID.
     */
    private String extractBankName(String fipId) {
        if (fipId == null) return null;
        // FIP IDs are like "HDFC-FIP", "ICICI-FIP", etc.
        return fipId.replace("-FIP", "").replace("-fip", "");
    }

    /**
     * Parse amount string to paisa.
     */
    private Long parseAmountToPaisa(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) return 0L;
        try {
            double amount = Double.parseDouble(amountStr.replaceAll("[^0-9.-]", ""));
            return Math.round(amount * 100);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Convert BankAccount entity to DTO.
     */
    private BankAccountDto toDto(BankAccount entity) {
        BankAccountDto dto = new BankAccountDto();
        dto.setId(entity.getId());
        dto.setProfileId(entity.getProfileId());
        dto.setBankName(entity.getBankName());
        dto.setMaskedAccountNumber(entity.getMaskedAccountNumber());
        dto.setAccountType(entity.getAccountType());
        dto.setAccountHolderName(entity.getAccountHolderName());
        dto.setBranch(entity.getBranch());
        dto.setIfsc(entity.getIfsc());
        dto.setConsentStatus(entity.getConsentStatus());
        dto.setConsentExpiresAt(entity.getConsentExpiresAt());
        dto.setLastSyncedAt(entity.getLastSyncedAt());
        if (entity.getCurrentBalance() != null) {
            dto.setCurrentBalance(entity.getCurrentBalance() / 100.0);
        }
        dto.setCurrency(entity.getCurrency());
        dto.setNickname(entity.getNickname());
        dto.setIsPrimary(entity.getIsPrimary());
        dto.setIsActive(entity.getIsActive());
        return dto;
    }

    /**
     * Get transactions for a bank account with optional date filtering.
     */
    public List<TransactionEntity> getTransactionsForAccount(
            UUID accountId, String fromDateStr, String toDateStr, int page, int size) {

        // Verify account exists
        bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found: " + accountId));

        if (fromDateStr != null && toDateStr != null) {
            LocalDate fromDate = LocalDate.parse(fromDateStr);
            LocalDate toDate = LocalDate.parse(toDateStr);
            return transactionRepository.findByBankAccountIdAndTxnDateBetweenOrderByTxnDateDesc(
                    accountId, fromDate, toDate);
        }

        return transactionRepository.findByBankAccountIdOrderByTxnDateDescCreatedAtDesc(accountId);
    }

    /**
     * Get sync history for a bank account.
     */
    public List<SyncHistory> getSyncHistory(UUID accountId, int limit) {
        // Verify account exists
        bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found: " + accountId));

        List<SyncHistory> history = syncHistoryRepository.findByBankAccountIdOrderByStartedAtDesc(accountId);

        if (history.size() > limit) {
            return history.subList(0, limit);
        }
        return history;
    }
}

