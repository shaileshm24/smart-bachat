package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.config.SetuConfig;
import com.ametsa.smartbachat.dto.BankAccountDto;
import com.ametsa.smartbachat.dto.BankConnectionRequestDto;
import com.ametsa.smartbachat.dto.BankConnectionResponseDto;
import com.ametsa.smartbachat.dto.setu.*;
import com.ametsa.smartbachat.entity.BankAccount;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.repository.BankAccountRepository;
import com.ametsa.smartbachat.repository.TransactionRepository;
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
    private final BankTransactionMapper transactionMapper;
    private final SetuConfig setuConfig;

    public BankConnectionService(
            SetuAggregatorService setuService,
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            BankTransactionMapper transactionMapper,
            SetuConfig setuConfig) {
        this.setuService = setuService;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.setuConfig = setuConfig;
    }

    /**
     * Initiate bank account connection.
     */
    @Transactional
    public BankConnectionResponseDto initiateConnection(BankConnectionRequestDto request) throws Exception {
        log.info("Initiating bank connection for profile: {}", request.getProfileId());

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
        bankAccount.setProfileId(request.getProfileId());
        bankAccount.setConsentId(consentResponse.getId());
        bankAccount.setConsentStatus("PENDING");
        bankAccount.setAggregator("SETU");
        bankAccount.setIsActive(false);
        bankAccount.setCreatedAt(Instant.now());
        bankAccount.setUpdatedAt(Instant.now());

        bankAccountRepository.save(bankAccount);

        log.info("Created bank account {} with consent {}", bankAccount.getId(), consentResponse.getId());

        return new BankConnectionResponseDto(
                bankAccount.getId(),
                consentResponse.getId(),
                consentResponse.getUrl(),
                "PENDING"
        );
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
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found: " + accountId));

        if (!"ACTIVE".equals(account.getConsentStatus())) {
            throw new RuntimeException("Consent is not active for this account");
        }

        log.info("Syncing account: {}", accountId);

        // Create data session
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = account.getLastSyncedAt() != null
                ? LocalDate.ofInstant(account.getLastSyncedAt(), java.time.ZoneId.systemDefault())
                : toDate.minusMonths(setuConfig.getDataFetchMonths());

        SetuDataSessionResponse sessionResponse = setuService.createDataSession(
                account.getConsentId(), fromDate, toDate);

        // Fetch data (in production, this would be async via webhook)
        SetuFIDataResponse dataResponse = setuService.fetchSessionData(sessionResponse.getId());

        // Process and save transactions
        int savedCount = processAndSaveTransactions(dataResponse, account);

        // Update account
        account.setLastSessionId(sessionResponse.getId());
        account.setLastSyncedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        bankAccountRepository.save(account);

        BankConnectionResponseDto response = new BankConnectionResponseDto();
        response.setBankAccountId(accountId);
        response.setStatus("SUCCESS");
        response.setMessage("Synced " + savedCount + " transactions");
        return response;
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
                .ifPresent(account -> {
                    account.setConsentStatus(payload.getStatus());
                    account.setUpdatedAt(Instant.now());

                    if ("ACTIVE".equals(payload.getStatus())) {
                        account.setIsActive(true);
                        account.setConsentApprovedAt(Instant.now());

                        // Update account details from linked accounts
                        if (payload.getAccounts() != null && !payload.getAccounts().isEmpty()) {
                            SetuWebhookPayload.LinkedAccount linked = payload.getAccounts().get(0);
                            account.setFipId(linked.getFipId());
                            account.setMaskedAccountNumber(linked.getMaskedAccNumber());
                            account.setFiType(linked.getFiType());
                            account.setLinkedAccountRef(linked.getLinkRefNumber());
                        }
                    }

                    bankAccountRepository.save(account);
                    log.info("Updated consent status for account: {}", account.getId());
                });
    }

    private void handleSessionStatusUpdate(SetuWebhookPayload payload) {
        // Session completed - could trigger async data fetch
        log.info("Session status update: sessionId={}, status={}",
                payload.getSessionId(), payload.getStatus());
    }

    /**
     * Process FI data response and save transactions.
     */
    private int processAndSaveTransactions(SetuFIDataResponse dataResponse, BankAccount account) {
        int savedCount = 0;

        if (dataResponse.getFips() == null) {
            return savedCount;
        }

        for (SetuFIDataResponse.FIPData fip : dataResponse.getFips()) {
            // Update bank name from FIP ID
            if (account.getBankName() == null && fip.getFipId() != null) {
                account.setBankName(extractBankName(fip.getFipId()));
            }

            if (fip.getAccounts() == null) continue;

            for (SetuFIDataResponse.AccountData accData : fip.getAccounts()) {
                // Update account details
                if (accData.getData() != null && accData.getData().getAccount() != null) {
                    SetuFIDataResponse.AccountInfo accInfo = accData.getData().getAccount();
                    account.setAccountType(accInfo.getType());
                    account.setBranch(accInfo.getBranch());
                    account.setIfsc(accInfo.getIfsc());
                    account.setCurrency(accInfo.getCurrency());

                    if (accInfo.getCurrentBalance() != null) {
                        account.setCurrentBalance(parseAmountToPaisa(accInfo.getCurrentBalance()));
                    }

                    if (accInfo.getHolder() != null) {
                        account.setAccountHolderName(accInfo.getHolder().getName());
                    }
                }

                // Process transactions
                if (accData.getData() != null &&
                    accData.getData().getTransactions() != null &&
                    accData.getData().getTransactions().getTransaction() != null) {

                    for (Transaction txn : accData.getData().getTransactions().getTransaction()) {
                        // Check for duplicates
                        if (txn.getTxnId() != null &&
                            transactionRepository.existsByBankAccountIdAndBankTxnId(
                                    account.getId(), txn.getTxnId())) {
                            continue;
                        }

                        TransactionEntity entity = transactionMapper.mapFromAA(
                                txn, account.getId(), account.getProfileId());
                        transactionRepository.save(entity);
                        savedCount++;
                    }
                }
            }
        }

        return savedCount;
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
}

