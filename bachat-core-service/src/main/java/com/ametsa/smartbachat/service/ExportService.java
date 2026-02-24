package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.export.ExportRequest;
import com.ametsa.smartbachat.entity.*;
import com.ametsa.smartbachat.repository.*;
import com.ametsa.smartbachat.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    private static final long PAISA_MULTIPLIER = 100L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TransactionRepository transactionRepository;
    private final UserBudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final BillReminderRepository billReminderRepository;
    private final SecurityUtils securityUtils;

    public ExportService(
            TransactionRepository transactionRepository,
            UserBudgetRepository budgetRepository,
            BudgetCategoryRepository budgetCategoryRepository,
            SavingsGoalRepository savingsGoalRepository,
            BillReminderRepository billReminderRepository,
            SecurityUtils securityUtils) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.billReminderRepository = billReminderRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Export transactions to CSV format.
     */
    public String exportTransactionsCsv(ExportRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        log.info("Exporting transactions for profile: {}", profileId);

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now().minusMonths(3);
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();

        List<TransactionEntity> transactions = transactionRepository
                .findByProfileIdAndTxnDateBetweenOrderByTxnDateDesc(profileId, startDate, endDate);

        // Apply filters
        if (request.getDirection() != null) {
            transactions = transactions.stream()
                    .filter(t -> request.getDirection().equals(t.getDirection()))
                    .collect(Collectors.toList());
        }
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            transactions = transactions.stream()
                    .filter(t -> request.getCategories().contains(t.getCategory()))
                    .collect(Collectors.toList());
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Description,Category,Amount (₹),Type,Merchant,Balance (₹)\n");

        for (TransactionEntity txn : transactions) {
            csv.append(formatCsvRow(
                    formatDate(txn.getTxnDate()),
                    escapeCsv(txn.getDescription()),
                    txn.getCategory() != null ? txn.getCategory() : "",
                    paisaToRupees(txn.getAmount()),
                    txn.getDirection(),
                    escapeCsv(txn.getMerchant()),
                    paisaToRupees(txn.getBalance())
            ));
        }

        log.info("Exported {} transactions", transactions.size());
        return csv.toString();
    }

    /**
     * Export savings goals to CSV format.
     */
    public String exportGoalsCsv() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        log.info("Exporting goals for profile: {}", profileId);

        List<SavingsGoal> goals = savingsGoalRepository.findByProfileIdOrderByCreatedAtDesc(profileId);

        StringBuilder csv = new StringBuilder();
        csv.append("Goal Name,Target Amount (₹),Current Amount (₹),Progress (%),Target Date,Status,Created At\n");

        for (SavingsGoal goal : goals) {
            double progress = goal.getTargetAmount() > 0
                    ? (goal.getCurrentAmount() * 100.0 / goal.getTargetAmount()) : 0;
            csv.append(formatCsvRow(
                    escapeCsv(goal.getName()),
                    paisaToRupees(goal.getTargetAmount()),
                    paisaToRupees(goal.getCurrentAmount()),
                    String.format("%.1f", progress),
                    formatDate(goal.getDeadline()),
                    goal.getStatus(),
                    goal.getCreatedAt() != null ? goal.getCreatedAt().toString() : ""
            ));
        }

        log.info("Exported {} goals", goals.size());
        return csv.toString();
    }

    /**
     * Export bill reminders to CSV format.
     */
    public String exportBillsCsv() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        log.info("Exporting bills for profile: {}", profileId);

        List<BillReminder> bills = billReminderRepository.findByProfileIdOrderByNextDueDateAsc(profileId);

        StringBuilder csv = new StringBuilder();
        csv.append("Bill Name,Category,Amount (₹),Frequency,Next Due Date,Payee,Active,Auto Pay\n");

        for (BillReminder bill : bills) {
            csv.append(formatCsvRow(
                    escapeCsv(bill.getName()),
                    bill.getCategory() != null ? bill.getCategory() : "",
                    paisaToRupees(bill.getAmount()),
                    bill.getFrequency(),
                    formatDate(bill.getNextDueDate()),
                    escapeCsv(bill.getPayee()),
                    bill.getIsActive() ? "Yes" : "No",
                    bill.getIsAutoPay() ? "Yes" : "No"
            ));
        }

        log.info("Exported {} bills", bills.size());
        return csv.toString();
    }

    // Helper methods
    private String formatCsvRow(Object... values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) row.append(",");
            row.append(values[i] != null ? values[i].toString() : "");
        }
        row.append("\n");
        return row.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "";
    }

    private String paisaToRupees(Long paisa) {
        if (paisa == null) return "0.00";
        return String.format("%.2f", paisa / (double) PAISA_MULTIPLIER);
    }
}

