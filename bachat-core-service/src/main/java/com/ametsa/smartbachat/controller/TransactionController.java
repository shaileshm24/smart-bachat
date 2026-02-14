package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.TransactionDto;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.repository.TransactionRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import com.ametsa.smartbachat.service.TransactionCategorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for unified transaction access.
 * Provides endpoints for the AI advisor service and other consumers.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionRepository transactionRepository;
    private final TransactionCategorizationService categorizationService;
    private final SecurityUtils securityUtils;

    public TransactionController(
            TransactionRepository transactionRepository,
            TransactionCategorizationService categorizationService,
            SecurityUtils securityUtils) {
        this.transactionRepository = transactionRepository;
        this.categorizationService = categorizationService;
        this.securityUtils = securityUtils;
    }

    /**
     * Get all transactions for the authenticated user.
     * Combines transactions from PDF uploads and bank API connections.
     *
     * @param startDate Optional start date filter (inclusive)
     * @param endDate Optional end date filter (inclusive)
     * @param category Optional category filter
     * @param direction Optional direction filter (CREDIT/DEBIT)
     * @param search Optional search text for description/merchant
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 50)
     * @return Paginated list of transactions with summary
     */
    @GetMapping
    public ResponseEntity<TransactionListResponse> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        UUID userId = securityUtils.requireCurrentUserId();
        log.info("Fetching transactions for user: {}, dateRange: {} to {}, category: {}, search: {}, page: {}, size: {}",
                userId, startDate, endDate, category, search, page, size);

        // Normalize empty strings to null for query
        String categoryFilter = (category != null && !category.trim().isEmpty()) ? category.trim() : null;
        String directionFilter = (direction != null && !direction.trim().isEmpty()) ? direction.trim() : null;
        String searchFilter = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Get total count for pagination
        long totalCount = transactionRepository.countByUserIdWithFilters(
                userId, startDate, endDate, categoryFilter, directionFilter, searchFilter);

        // Get paginated transactions
        Pageable pageable = PageRequest.of(page, size);
        List<TransactionEntity> transactions = transactionRepository.findByUserIdWithFilters(
                userId, startDate, endDate, categoryFilter, directionFilter, searchFilter, pageable);

        // Ensure all transactions are categorized
        for (TransactionEntity txn : transactions) {
            categorizationService.categorizeAndUpdate(txn);
        }

        // Convert to DTOs
        List<TransactionDto> dtos = transactions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // Calculate summary (for current page)
        TransactionSummary summary = calculateSummary(transactions);

        // Build response with pagination info
        TransactionListResponse response = new TransactionListResponse();
        response.setTransactions(dtos);
        response.setSummary(summary);
        response.setTotalCount((int) totalCount);
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages((int) Math.ceil((double) totalCount / size));

        return ResponseEntity.ok(response);
    }

    /**
     * Get spending breakdown by category.
     */
    @GetMapping("/spending-by-category")
    public ResponseEntity<List<CategorySpending>> getSpendingByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID profileId = securityUtils.requireCurrentProfileId();

        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusMonths(6);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();

        List<TransactionEntity> transactions = transactionRepository
                .findByProfileIdAndTxnDateBetweenOrderByTxnDateDesc(profileId, effectiveStart, effectiveEnd);

        // Ensure categorization
        transactions.forEach(categorizationService::categorizeAndUpdate);

        // Group by category (only debits)
        Map<String, List<TransactionEntity>> byCategory = transactions.stream()
                .filter(t -> "DEBIT".equalsIgnoreCase(t.getDirection()))
                .collect(Collectors.groupingBy(t -> t.getCategory() != null ? t.getCategory() : "OTHER"));

        List<CategorySpending> result = byCategory.entrySet().stream()
                .map(entry -> {
                    CategorySpending cs = new CategorySpending();
                    cs.setCategory(entry.getKey());
                    cs.setTransactionCount(entry.getValue().size());
                    long totalPaisa = entry.getValue().stream()
                            .mapToLong(t -> t.getAmount() != null ? Math.abs(t.getAmount()) : 0)
                            .sum();
                    cs.setTotalAmount(totalPaisa / 100.0);
                    return cs;
                })
                .sorted((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private TransactionDto toDto(TransactionEntity e) {
        TransactionDto dto = new TransactionDto();
        dto.setId(e.getId());
        dto.setStatementId(e.getStatementId());
        dto.setProfileId(e.getProfileId());
        dto.setTxnDate(e.getTxnDate());
        if (e.getAmount() != null) {
            dto.setAmount(Math.abs(e.getAmount()) / 100.0);
        }
        dto.setDirection(e.getDirection());
        dto.setCurrency(e.getCurrency() != null ? e.getCurrency() : "INR");
        dto.setTxnType(e.getTxnType());
        dto.setDescription(e.getDescription());
        dto.setMerchant(e.getMerchant());
        if (e.getBalance() != null) {
            dto.setBalance(e.getBalance() / 100.0);
        }
        dto.setCategory(e.getCategory());
        dto.setSubCategory(e.getSubCategory());
        return dto;
    }

    private TransactionSummary calculateSummary(List<TransactionEntity> transactions) {
        TransactionSummary summary = new TransactionSummary();

        long totalCredits = 0;
        long totalDebits = 0;
        int creditCount = 0;
        int debitCount = 0;

        for (TransactionEntity txn : transactions) {
            long amount = txn.getAmount() != null ? Math.abs(txn.getAmount()) : 0;
            if ("CREDIT".equalsIgnoreCase(txn.getDirection())) {
                totalCredits += amount;
                creditCount++;
            } else if ("DEBIT".equalsIgnoreCase(txn.getDirection())) {
                totalDebits += amount;
                debitCount++;
            }
        }

        summary.setTotalIncome(totalCredits / 100.0);
        summary.setTotalExpenses(totalDebits / 100.0);
        summary.setNetSavings((totalCredits - totalDebits) / 100.0);
        summary.setCreditCount(creditCount);
        summary.setDebitCount(debitCount);

        return summary;
    }

    // Response DTOs
    public static class TransactionListResponse {
        private List<TransactionDto> transactions;
        private TransactionSummary summary;
        private int totalCount;
        private int page;
        private int size;
        private int totalPages;

        public List<TransactionDto> getTransactions() { return transactions; }
        public void setTransactions(List<TransactionDto> transactions) { this.transactions = transactions; }
        public TransactionSummary getSummary() { return summary; }
        public void setSummary(TransactionSummary summary) { this.summary = summary; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    public static class TransactionSummary {
        private Double totalIncome;
        private Double totalExpenses;
        private Double netSavings;
        private int creditCount;
        private int debitCount;

        public Double getTotalIncome() { return totalIncome; }
        public void setTotalIncome(Double totalIncome) { this.totalIncome = totalIncome; }
        public Double getTotalExpenses() { return totalExpenses; }
        public void setTotalExpenses(Double totalExpenses) { this.totalExpenses = totalExpenses; }
        public Double getNetSavings() { return netSavings; }
        public void setNetSavings(Double netSavings) { this.netSavings = netSavings; }
        public int getCreditCount() { return creditCount; }
        public void setCreditCount(int creditCount) { this.creditCount = creditCount; }
        public int getDebitCount() { return debitCount; }
        public void setDebitCount(int debitCount) { this.debitCount = debitCount; }
    }

    public static class CategorySpending {
        private String category;
        private Double totalAmount;
        private int transactionCount;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
        public int getTransactionCount() { return transactionCount; }
        public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
    }
}

