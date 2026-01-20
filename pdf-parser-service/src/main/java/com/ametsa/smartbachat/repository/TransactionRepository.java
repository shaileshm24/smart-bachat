package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    List<TransactionEntity> findByStatementIdOrderByTxnDateAscCreatedAtAsc(UUID statementId);

    /**
     * Find transactions by bank account ID.
     */
    List<TransactionEntity> findByBankAccountIdOrderByTxnDateDescCreatedAtDesc(UUID bankAccountId);

    /**
     * Find transactions by profile ID.
     */
    List<TransactionEntity> findByProfileIdOrderByTxnDateDescCreatedAtDesc(UUID profileId);

    /**
     * Find transactions by profile and date range.
     */
    List<TransactionEntity> findByProfileIdAndTxnDateBetweenOrderByTxnDateDesc(
            UUID profileId, LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by bank account and date range.
     */
    List<TransactionEntity> findByBankAccountIdAndTxnDateBetweenOrderByTxnDateDesc(
            UUID bankAccountId, LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by category.
     */
    List<TransactionEntity> findByProfileIdAndCategoryOrderByTxnDateDesc(UUID profileId, String category);

    /**
     * Find by bank transaction ID (for deduplication).
     */
    Optional<TransactionEntity> findByBankAccountIdAndBankTxnId(UUID bankAccountId, String bankTxnId);

    /**
     * Check if transaction exists (for deduplication).
     */
    boolean existsByBankAccountIdAndBankTxnId(UUID bankAccountId, String bankTxnId);

    /**
     * Find transactions by source type.
     */
    List<TransactionEntity> findByProfileIdAndSourceTypeOrderByTxnDateDesc(UUID profileId, String sourceType);

    /**
     * Get total credit amount for a profile in date range.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t " +
           "WHERE t.profileId = :profileId AND t.direction = 'CREDIT' " +
           "AND t.txnDate BETWEEN :startDate AND :endDate")
    Long sumCreditsByProfileAndDateRange(
            @Param("profileId") UUID profileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total debit amount for a profile in date range.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t " +
           "WHERE t.profileId = :profileId AND t.direction = 'DEBIT' " +
           "AND t.txnDate BETWEEN :startDate AND :endDate")
    Long sumDebitsByProfileAndDateRange(
            @Param("profileId") UUID profileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}

