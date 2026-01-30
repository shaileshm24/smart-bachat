package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks sync history for bank accounts.
 * Records each sync attempt with status, counts, and error details.
 */
@Entity
@Table(name = "sync_history", indexes = {
        @Index(name = "idx_sync_history_user", columnList = "user_id"),
        @Index(name = "idx_sync_history_account", columnList = "bank_account_id"),
        @Index(name = "idx_sync_history_status", columnList = "status"),
        @Index(name = "idx_sync_history_started", columnList = "started_at")
})
public class SyncHistory {

    @Id
    @Column(name = "id")
    private UUID id;

    // User ID from UAM service (logged-in user)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "bank_account_id", nullable = false)
    private UUID bankAccountId;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    // PENDING, IN_PROGRESS, SUCCESS, FAILED, PARTIAL
    @Column(name = "status", nullable = false)
    private String status;

    // Setu session ID for this sync
    @Column(name = "session_id")
    private String sessionId;

    // Sync trigger: MANUAL, SCHEDULED, WEBHOOK
    @Column(name = "trigger_type")
    private String triggerType;

    // Timestamps
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Counts
    @Column(name = "transactions_fetched")
    private Integer transactionsFetched;

    @Column(name = "transactions_saved")
    private Integer transactionsSaved;

    @Column(name = "transactions_skipped")
    private Integer transactionsSkipped;

    // Date range of fetched data
    @Column(name = "data_from_date")
    private Instant dataFromDate;

    @Column(name = "data_to_date")
    private Instant dataToDate;

    // Error details
    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    // Retry information
    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    public SyncHistory() {
        this.id = UUID.randomUUID();
        this.startedAt = Instant.now();
        this.status = "PENDING";
        this.retryCount = 0;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(UUID bankAccountId) { this.bankAccountId = bankAccountId; }
    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Integer getTransactionsFetched() { return transactionsFetched; }
    public void setTransactionsFetched(Integer transactionsFetched) { this.transactionsFetched = transactionsFetched; }
    public Integer getTransactionsSaved() { return transactionsSaved; }
    public void setTransactionsSaved(Integer transactionsSaved) { this.transactionsSaved = transactionsSaved; }
    public Integer getTransactionsSkipped() { return transactionsSkipped; }
    public void setTransactionsSkipped(Integer transactionsSkipped) { this.transactionsSkipped = transactionsSkipped; }
    public Instant getDataFromDate() { return dataFromDate; }
    public void setDataFromDate(Instant dataFromDate) { this.dataFromDate = dataFromDate; }
    public Instant getDataToDate() { return dataToDate; }
    public void setDataToDate(Instant dataToDate) { this.dataToDate = dataToDate; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    // Helper methods
    public void markSuccess(int fetched, int saved, int skipped) {
        this.status = "SUCCESS";
        this.completedAt = Instant.now();
        this.transactionsFetched = fetched;
        this.transactionsSaved = saved;
        this.transactionsSkipped = skipped;
    }

    public void markFailed(String errorCode, String errorMessage) {
        this.status = "FAILED";
        this.completedAt = Instant.now();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public void markInProgress(String sessionId) {
        this.status = "IN_PROGRESS";
        this.sessionId = sessionId;
    }
}

