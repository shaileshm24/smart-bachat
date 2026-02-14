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
    List<TransactionEntity> findByUserIdOrderByTxnDateDescCreatedAtDesc(UUID userId);

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
     * Find transactions by user id and date range.
     */
    List<TransactionEntity> findByBankAccountIdAndTxnDateBetweenOrderByTxnDateDesc(
            UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by category.
     */
    List<TransactionEntity> findByProfileIdAndCategoryOrderByTxnDateDesc(UUID profileId, String category);

    /**
     * Find by bank transaction ID (for deduplication).
     */
    Optional<TransactionEntity> findByBankAccountIdAndBankTxnId(UUID bankAccountId, String bankTxnId);

    /**
     * Check if transaction exists by bank txn ID (for deduplication).
     */
    boolean existsByBankAccountIdAndBankTxnId(UUID bankAccountId, String bankTxnId);

    /**
     * Check if transaction exists by dedupe key (for deduplication).
     * This is more reliable than bankTxnId as it combines multiple fields.
     */
    boolean existsByDedupeKey(String dedupeKey);

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

    /**
     * Get recent transactions for a profile (limited to top N).
     * Used for dashboard display.
     */
    List<TransactionEntity> findTop5ByProfileIdOrderByTxnDateDescCreatedAtDesc(UUID profileId);

    /**
     * Get recent transactions for a profile with configurable limit.
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.profileId = :profileId " +
           "ORDER BY t.txnDate DESC, t.createdAt DESC LIMIT :limit")
    List<TransactionEntity> findRecentByProfileId(
            @Param("profileId") UUID profileId,
            @Param("limit") int limit);

    /**
     * Find transactions by user ID with date range.
     */
    List<TransactionEntity> findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(
            UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Count transactions by user ID with optional filters.
     */
    @Query(value = "SELECT COUNT(*) FROM transactions t WHERE t.user_id = :userId " +
           "AND (:startDate IS NULL OR t.txn_date >= :startDate) " +
           "AND (:endDate IS NULL OR t.txn_date <= :endDate) " +
           "AND (:category IS NULL OR t.category = :category) " +
           "AND (:direction IS NULL OR t.direction = :direction) " +
           "AND (:search IS NULL OR LOWER(CAST(t.description AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(CAST(t.merchant AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    long countByUserIdWithFilters(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category,
            @Param("direction") String direction,
            @Param("search") String search);

    /**
     * Find transactions by user ID with optional filters and pagination.
     */
    @Query(value = "SELECT * FROM transactions t WHERE t.user_id = :userId " +
           "AND (:startDate IS NULL OR t.txn_date >= :startDate) " +
           "AND (:endDate IS NULL OR t.txn_date <= :endDate) " +
           "AND (:category IS NULL OR t.category = :category) " +
           "AND (:direction IS NULL OR t.direction = :direction) " +
           "AND (:search IS NULL OR LOWER(CAST(t.description AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(CAST(t.merchant AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.txn_date DESC, t.created_at DESC",
           nativeQuery = true)
    List<TransactionEntity> findByUserIdWithFilters(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category,
            @Param("direction") String direction,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable);
}

