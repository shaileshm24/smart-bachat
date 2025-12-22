package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    /**
     * Find all bank accounts for a profile.
     */
    List<BankAccount> findByProfileIdOrderByCreatedAtDesc(UUID profileId);

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
}

