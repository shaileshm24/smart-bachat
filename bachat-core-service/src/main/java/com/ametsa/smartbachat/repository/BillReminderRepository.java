package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.BillReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillReminderRepository extends JpaRepository<BillReminder, UUID> {

    /**
     * Find all reminders for a profile.
     */
    List<BillReminder> findByProfileIdOrderByNextDueDateAsc(UUID profileId);

    /**
     * Find active reminders for a profile.
     */
    List<BillReminder> findByProfileIdAndIsActiveTrueOrderByNextDueDateAsc(UUID profileId);

    /**
     * Find reminder by ID and profile (for security).
     */
    Optional<BillReminder> findByIdAndProfileId(UUID id, UUID profileId);

    /**
     * Count active reminders for a profile (for free tier limit).
     */
    long countByProfileIdAndIsActiveTrue(UUID profileId);

    /**
     * Find reminders due within a date range.
     */
    @Query("SELECT br FROM BillReminder br WHERE br.profileId = :profileId " +
           "AND br.isActive = true AND br.nextDueDate BETWEEN :startDate AND :endDate " +
           "ORDER BY br.nextDueDate ASC")
    List<BillReminder> findDueReminders(
            @Param("profileId") UUID profileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find overdue reminders.
     */
    @Query("SELECT br FROM BillReminder br WHERE br.profileId = :profileId " +
           "AND br.isActive = true AND br.nextDueDate < :today " +
           "ORDER BY br.nextDueDate ASC")
    List<BillReminder> findOverdueReminders(
            @Param("profileId") UUID profileId,
            @Param("today") LocalDate today);

    /**
     * Find reminders by category.
     */
    List<BillReminder> findByProfileIdAndCategoryOrderByNextDueDateAsc(UUID profileId, String category);
}

