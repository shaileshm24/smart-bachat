package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.bills.*;
import com.ametsa.smartbachat.entity.BillReminder;
import com.ametsa.smartbachat.repository.BillReminderRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BillReminderService {

    private static final Logger log = LoggerFactory.getLogger(BillReminderService.class);
    private static final long PAISA_MULTIPLIER = 100L;
    private static final int FREE_TIER_MAX_BILLS = 5;

    private final BillReminderRepository reminderRepository;
    private final SecurityUtils securityUtils;

    public BillReminderService(BillReminderRepository reminderRepository, SecurityUtils securityUtils) {
        this.reminderRepository = reminderRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Create a new bill reminder.
     */
    @Transactional
    public BillReminderResponse createReminder(CreateBillReminderRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();

        // Check free tier limit
        long activeCount = reminderRepository.countByProfileIdAndIsActiveTrue(profileId);
        if (activeCount >= FREE_TIER_MAX_BILLS) {
            throw new RuntimeException("Free tier limit reached. Maximum " + FREE_TIER_MAX_BILLS + " bill reminders allowed.");
        }

        BillReminder reminder = new BillReminder();
        reminder.setProfileId(profileId);
        reminder.setName(request.getName());
        reminder.setCategory(request.getCategory());
        reminder.setAmount(rupeesToPaisa(request.getAmount()));
        reminder.setFrequency(request.getFrequency());
        reminder.setCustomDays(request.getCustomDays());
        reminder.setNextDueDate(request.getNextDueDate());
        reminder.setReminderDaysBefore(request.getReminderDaysBefore() != null ? request.getReminderDaysBefore() : 3);
        reminder.setIsAutoPay(request.getIsAutoPay() != null ? request.getIsAutoPay() : false);
        reminder.setPayee(request.getPayee());
        reminder.setAccountReference(request.getAccountReference());
        reminder.setIcon(request.getIcon());
        reminder.setColor(request.getColor());
        reminder.setNotes(request.getNotes());

        reminder = reminderRepository.save(reminder);
        log.info("Created bill reminder: {}", reminder.getName());

        return mapToResponse(reminder);
    }

    /**
     * Get all reminders for the user.
     */
    public List<BillReminderResponse> getAllReminders() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        return reminderRepository.findByProfileIdOrderByNextDueDateAsc(profileId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active reminders only.
     */
    public List<BillReminderResponse> getActiveReminders() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        return reminderRepository.findByProfileIdAndIsActiveTrueOrderByNextDueDateAsc(profileId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific reminder.
     */
    public BillReminderResponse getReminder(UUID reminderId) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        BillReminder reminder = reminderRepository.findByIdAndProfileId(reminderId, profileId)
                .orElseThrow(() -> new RuntimeException("Bill reminder not found"));
        return mapToResponse(reminder);
    }

    /**
     * Update a reminder.
     */
    @Transactional
    public BillReminderResponse updateReminder(UUID reminderId, UpdateBillReminderRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        BillReminder reminder = reminderRepository.findByIdAndProfileId(reminderId, profileId)
                .orElseThrow(() -> new RuntimeException("Bill reminder not found"));

        if (request.getName() != null) reminder.setName(request.getName());
        if (request.getCategory() != null) reminder.setCategory(request.getCategory());
        if (request.getAmount() != null) reminder.setAmount(rupeesToPaisa(request.getAmount()));
        if (request.getFrequency() != null) reminder.setFrequency(request.getFrequency());
        if (request.getCustomDays() != null) reminder.setCustomDays(request.getCustomDays());
        if (request.getNextDueDate() != null) reminder.setNextDueDate(request.getNextDueDate());
        if (request.getReminderDaysBefore() != null) reminder.setReminderDaysBefore(request.getReminderDaysBefore());
        if (request.getIsActive() != null) reminder.setIsActive(request.getIsActive());
        if (request.getIsAutoPay() != null) reminder.setIsAutoPay(request.getIsAutoPay());
        if (request.getPayee() != null) reminder.setPayee(request.getPayee());
        if (request.getAccountReference() != null) reminder.setAccountReference(request.getAccountReference());
        if (request.getIcon() != null) reminder.setIcon(request.getIcon());
        if (request.getColor() != null) reminder.setColor(request.getColor());
        if (request.getNotes() != null) reminder.setNotes(request.getNotes());

        reminder.setUpdatedAt(Instant.now());
        reminder = reminderRepository.save(reminder);
        log.info("Updated bill reminder: {}", reminderId);

        return mapToResponse(reminder);
    }

    /**
     * Delete a reminder.
     */
    @Transactional
    public void deleteReminder(UUID reminderId) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        BillReminder reminder = reminderRepository.findByIdAndProfileId(reminderId, profileId)
                .orElseThrow(() -> new RuntimeException("Bill reminder not found"));
        reminderRepository.delete(reminder);
        log.info("Deleted bill reminder: {}", reminderId);
    }

    // Helper methods
    private Long rupeesToPaisa(Double rupees) {
        return rupees != null ? Math.round(rupees * PAISA_MULTIPLIER) : 0L;
    }

    private Double paisaToRupees(Long paisa) {
        return paisa != null ? paisa / (double) PAISA_MULTIPLIER : 0.0;
    }

    /**
     * Mark a bill as paid and calculate next due date.
     */
    @Transactional
    public BillReminderResponse markAsPaid(UUID reminderId, MarkBillPaidRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        BillReminder reminder = reminderRepository.findByIdAndProfileId(reminderId, profileId)
                .orElseThrow(() -> new RuntimeException("Bill reminder not found"));

        LocalDate paidDate = request.getPaidDate() != null ? request.getPaidDate() : LocalDate.now();
        Double paidAmount = request.getPaidAmount() != null ? request.getPaidAmount() : paisaToRupees(reminder.getAmount());

        reminder.setLastPaidDate(paidDate);
        reminder.setLastPaidAmount(rupeesToPaisa(paidAmount));
        reminder.setNextDueDate(calculateNextDueDate(reminder));
        reminder.setUpdatedAt(Instant.now());

        reminder = reminderRepository.save(reminder);
        log.info("Marked bill as paid: {} on {}", reminder.getName(), paidDate);

        return mapToResponse(reminder);
    }

    /**
     * Get bills summary with overdue, due soon, and upcoming bills.
     */
    public BillsSummaryResponse getSummary() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);

        List<BillReminder> allReminders = reminderRepository.findByProfileIdOrderByNextDueDateAsc(profileId);
        List<BillReminder> activeReminders = allReminders.stream()
                .filter(BillReminder::getIsActive)
                .collect(Collectors.toList());

        List<BillReminderResponse> overdueBills = activeReminders.stream()
                .filter(r -> r.getNextDueDate().isBefore(today))
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        List<BillReminderResponse> dueSoonBills = activeReminders.stream()
                .filter(r -> !r.getNextDueDate().isBefore(today) && !r.getNextDueDate().isAfter(weekFromNow))
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        List<BillReminderResponse> upcomingBills = activeReminders.stream()
                .filter(r -> r.getNextDueDate().isAfter(weekFromNow))
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        double totalMonthly = calculateMonthlyTotal(activeReminders);
        double totalOverdue = overdueBills.stream().mapToDouble(BillReminderResponse::getAmount).sum();
        double totalDueSoon = dueSoonBills.stream().mapToDouble(BillReminderResponse::getAmount).sum();

        long activeCount = reminderRepository.countByProfileIdAndIsActiveTrue(profileId);

        return BillsSummaryResponse.builder()
                .totalBills(allReminders.size())
                .activeBills(activeReminders.size())
                .overdueCount(overdueBills.size())
                .dueSoonCount(dueSoonBills.size())
                .totalMonthlyAmount(totalMonthly)
                .totalOverdueAmount(totalOverdue)
                .totalDueSoonAmount(totalDueSoon)
                .overdueBills(overdueBills)
                .dueSoonBills(dueSoonBills)
                .upcomingBills(upcomingBills)
                .maxBillsAllowed(FREE_TIER_MAX_BILLS)
                .remainingSlots((int) (FREE_TIER_MAX_BILLS - activeCount))
                .build();
    }

    /**
     * Calculate next due date based on frequency.
     */
    private LocalDate calculateNextDueDate(BillReminder reminder) {
        LocalDate currentDue = reminder.getNextDueDate();
        String frequency = reminder.getFrequency();

        return switch (frequency) {
            case "WEEKLY" -> currentDue.plusWeeks(1);
            case "MONTHLY" -> currentDue.plusMonths(1);
            case "QUARTERLY" -> currentDue.plusMonths(3);
            case "YEARLY" -> currentDue.plusYears(1);
            case "CUSTOM" -> {
                int days = reminder.getCustomDays() != null ? reminder.getCustomDays() : 30;
                yield currentDue.plusDays(days);
            }
            default -> currentDue.plusMonths(1);
        };
    }

    /**
     * Calculate total monthly amount from all active reminders.
     */
    private double calculateMonthlyTotal(List<BillReminder> reminders) {
        return reminders.stream()
                .mapToDouble(r -> {
                    double amount = paisaToRupees(r.getAmount());
                    return switch (r.getFrequency()) {
                        case "WEEKLY" -> amount * 4.33;  // Average weeks per month
                        case "MONTHLY" -> amount;
                        case "QUARTERLY" -> amount / 3;
                        case "YEARLY" -> amount / 12;
                        case "CUSTOM" -> {
                            int days = r.getCustomDays() != null ? r.getCustomDays() : 30;
                            yield amount * (30.0 / days);
                        }
                        default -> amount;
                    };
                })
                .sum();
    }

    private BillReminderResponse mapToResponse(BillReminder reminder) {
        LocalDate today = LocalDate.now();
        int daysUntilDue = (int) ChronoUnit.DAYS.between(today, reminder.getNextDueDate());
        boolean isOverdue = daysUntilDue < 0;

        String status;
        if (isOverdue) {
            status = "OVERDUE";
        } else if (daysUntilDue <= 7) {
            status = "DUE_SOON";
        } else {
            status = "UPCOMING";
        }

        return BillReminderResponse.builder()
                .id(reminder.getId())
                .name(reminder.getName())
                .category(reminder.getCategory())
                .amount(paisaToRupees(reminder.getAmount()))
                .frequency(reminder.getFrequency())
                .customDays(reminder.getCustomDays())
                .nextDueDate(reminder.getNextDueDate())
                .reminderDaysBefore(reminder.getReminderDaysBefore())
                .isActive(reminder.getIsActive())
                .isAutoPay(reminder.getIsAutoPay())
                .payee(reminder.getPayee())
                .accountReference(reminder.getAccountReference())
                .icon(reminder.getIcon())
                .color(reminder.getColor())
                .notes(reminder.getNotes())
                .daysUntilDue(daysUntilDue)
                .isOverdue(isOverdue)
                .status(status)
                .lastPaidDate(reminder.getLastPaidDate())
                .lastPaidAmount(paisaToRupees(reminder.getLastPaidAmount()))
                .createdAt(reminder.getCreatedAt())
                .updatedAt(reminder.getUpdatedAt())
                .build();
    }
}

