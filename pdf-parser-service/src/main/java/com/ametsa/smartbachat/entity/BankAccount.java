package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a connected bank account via Account Aggregator.
 * Stores consent information and account details.
 */
@Entity
@Table(name = "bank_accounts", indexes = {
        @Index(name = "idx_bank_account_profile", columnList = "profile_id"),
        @Index(name = "idx_bank_account_consent", columnList = "consent_id"),
        @Index(name = "idx_bank_account_status", columnList = "consent_status")
})
public class BankAccount {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    // Bank identifier (e.g., HDFC-FIP, ICICI-FIP, SBI-FIP)
    @Column(name = "fip_id")
    private String fipId;

    // Human-readable bank name
    @Column(name = "bank_name")
    private String bankName;

    // Masked account number (e.g., XXXX1234)
    @Column(name = "masked_account_number")
    private String maskedAccountNumber;

    // Account type: SAVINGS, CURRENT, DEPOSIT, etc.
    @Column(name = "account_type")
    private String accountType;

    // FI Type from AA: DEPOSIT, TERM_DEPOSIT, RECURRING_DEPOSIT, etc.
    @Column(name = "fi_type")
    private String fiType;

    // Account holder name (if available)
    @Column(name = "account_holder_name")
    private String accountHolderName;

    // Branch name
    @Column(name = "branch")
    private String branch;

    // IFSC code
    @Column(name = "ifsc")
    private String ifsc;

    // ========== CONSENT INFORMATION ==========

    // Consent ID from AA (Setu/Finvu)
    @Column(name = "consent_id")
    private String consentId;

    // Consent handle (used during consent flow)
    @Column(name = "consent_handle")
    private String consentHandle;

    // Consent status: PENDING, ACTIVE, PAUSED, REVOKED, EXPIRED, REJECTED
    @Column(name = "consent_status")
    private String consentStatus;

    // When consent was approved
    @Column(name = "consent_approved_at")
    private Instant consentApprovedAt;

    // When consent expires
    @Column(name = "consent_expires_at")
    private Instant consentExpiresAt;

    // ========== DATA SESSION INFORMATION ==========

    // Last successful data session ID
    @Column(name = "last_session_id")
    private String lastSessionId;

    // Last successful sync timestamp
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    // Current balance in paisa (last known)
    @Column(name = "current_balance")
    private Long currentBalance;

    // Currency code (INR)
    @Column(name = "currency")
    private String currency;

    // ========== AGGREGATOR INFORMATION ==========

    // Which aggregator: SETU, FINVU, ONEMONEY
    @Column(name = "aggregator")
    private String aggregator;

    // Linked account ID from aggregator
    @Column(name = "linked_account_ref")
    private String linkedAccountRef;

    // ========== METADATA ==========

    // User-defined nickname for the account
    @Column(name = "nickname")
    private String nickname;

    // Is this the primary account?
    @Column(name = "is_primary")
    private Boolean isPrimary;

    // Is account active for syncing?
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Error message if last sync failed
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    public BankAccount() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }
    public String getFipId() { return fipId; }
    public void setFipId(String fipId) { this.fipId = fipId; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public void setMaskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getFiType() { return fiType; }
    public void setFiType(String fiType) { this.fiType = fiType; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getConsentHandle() { return consentHandle; }
    public void setConsentHandle(String consentHandle) { this.consentHandle = consentHandle; }
    public String getConsentStatus() { return consentStatus; }
    public void setConsentStatus(String consentStatus) { this.consentStatus = consentStatus; }
    public Instant getConsentApprovedAt() { return consentApprovedAt; }
    public void setConsentApprovedAt(Instant consentApprovedAt) { this.consentApprovedAt = consentApprovedAt; }
    public Instant getConsentExpiresAt() { return consentExpiresAt; }
    public void setConsentExpiresAt(Instant consentExpiresAt) { this.consentExpiresAt = consentExpiresAt; }
    public String getLastSessionId() { return lastSessionId; }
    public void setLastSessionId(String lastSessionId) { this.lastSessionId = lastSessionId; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public Long getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(Long currentBalance) { this.currentBalance = currentBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getAggregator() { return aggregator; }
    public void setAggregator(String aggregator) { this.aggregator = aggregator; }
    public String getLinkedAccountRef() { return linkedAccountRef; }
    public void setLinkedAccountRef(String linkedAccountRef) { this.linkedAccountRef = linkedAccountRef; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

