package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    /**
     * Find all bank accounts for a profile.
     */
    List<BankAccount> findByProfileIdOrderByCreatedAtDesc(UUID profileId);

    List<BankAccount> findByUserId(UUID userId);
    /**
     * Find all active bank accounts for a profile.
     */
    List<BankAccount> findByProfileIdAndIsActiveTrue(UUID profileId);

    /**
     * Find bank account by consent ID.
     */
    Optional<BankAccount> findByConsentId(String consentId);

    /**
     * Find bank account by consent handle.
     */
    Optional<BankAccount> findByConsentHandle(String consentHandle);

    /**
     * Find all accounts with active consent status.
     */
    List<BankAccount> findByConsentStatus(String consentStatus);

    /**
     * Find accounts by profile and consent status.
     */
    List<BankAccount> findByProfileIdAndConsentStatus(UUID profileId, String consentStatus);

    /**
     * Check if account exists for profile with given masked account number.
     */
    boolean existsByProfileIdAndMaskedAccountNumber(UUID profileId, String maskedAccountNumber);

    /**
     * Find bank account by profile and masked account number.
     * Used to match Setu response accounts to our BankAccount records.
     */
    Optional<BankAccount> findByProfileIdAndMaskedAccountNumber(UUID profileId, String maskedAccountNumber);

    /**
     * Find all bank accounts with the same consent ID.
     * Used when syncing to get all accounts linked to a consent.
     */
    List<BankAccount> findByConsentIdAndIsActiveTrue(String consentId);

    /**
     * Get total balance across all active accounts for a profile.
     * Used for dashboard display.
     */
    @Query("SELECT COALESCE(SUM(b.currentBalance), 0) FROM BankAccount b " +
           "WHERE b.profileId = :profileId AND b.isActive = true AND b.consentStatus = 'ACTIVE'")
    Long sumBalanceByProfileId(@Param("profileId") UUID profileId);

    /**
     * Count active accounts for a profile.
     */
    @Query("SELECT COUNT(b) FROM BankAccount b " +
           "WHERE b.profileId = :profileId AND b.isActive = true AND b.consentStatus = 'ACTIVE'")
    Integer countActiveAccountsByProfileId(@Param("profileId") UUID profileId);

    /**
     * Get the most recent sync time across all accounts for a profile.
     */
    @Query("SELECT MAX(b.lastSyncedAt) FROM BankAccount b " +
           "WHERE b.profileId = :profileId AND b.isActive = true")
    java.time.Instant getLastSyncTimeByProfileId(@Param("profileId") UUID profileId);
}

