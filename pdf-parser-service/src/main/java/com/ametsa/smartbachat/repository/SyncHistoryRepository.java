package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.SyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyncHistoryRepository extends JpaRepository<SyncHistory, UUID> {

    /**
     * Find sync history for a bank account, ordered by most recent first.
     */
    List<SyncHistory> findByBankAccountIdOrderByStartedAtDesc(UUID bankAccountId);

    /**
     * Find sync history for a profile.
     */
    List<SyncHistory> findByProfileIdOrderByStartedAtDesc(UUID profileId);

    /**
     * Find the most recent sync for a bank account.
     */
    Optional<SyncHistory> findFirstByBankAccountIdOrderByStartedAtDesc(UUID bankAccountId);

    /**
     * Find sync history by status.
     */
    List<SyncHistory> findByStatusOrderByStartedAtDesc(String status);

    /**
     * Find failed syncs that need retry.
     */
    List<SyncHistory> findByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(String status, Instant now);

    /**
     * Find sync history by session ID.
     */
    Optional<SyncHistory> findBySessionId(String sessionId);

    /**
     * Count syncs by status for a bank account.
     */
    long countByBankAccountIdAndStatus(UUID bankAccountId, String status);

    /**
     * Find recent syncs within a time range.
     */
    List<SyncHistory> findByBankAccountIdAndStartedAtAfterOrderByStartedAtDesc(
            UUID bankAccountId, Instant after);

    /**
     * Get total transactions synced for a bank account.
     */
    @Query("SELECT COALESCE(SUM(s.transactionsSaved), 0) FROM SyncHistory s " +
           "WHERE s.bankAccountId = :bankAccountId AND s.status = 'SUCCESS'")
    Long sumTransactionsSavedByBankAccountId(@Param("bankAccountId") UUID bankAccountId);

    /**
     * Delete old sync history (for cleanup).
     */
    void deleteByStartedAtBefore(Instant before);
}

