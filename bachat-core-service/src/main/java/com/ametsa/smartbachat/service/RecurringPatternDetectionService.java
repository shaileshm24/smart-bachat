package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.UncategorizedPatternDto;
import com.ametsa.smartbachat.entity.PatternType;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.repository.TransactionRepository;
import com.ametsa.smartbachat.repository.UserCategoryMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting recurring transaction patterns that are not yet categorized.
 * Helps users identify and categorize repetitive transactions like:
 * - UPI payments to the same mobile number
 * - Transfers to the same person
 * - Recurring subscriptions
 */
@Service
public class RecurringPatternDetectionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringPatternDetectionService.class);

    private final TransactionRepository transactionRepository;
    private final UserCategoryMappingRepository mappingRepository;
    private final TransactionCategorizationService categorizationService;

    public RecurringPatternDetectionService(
            TransactionRepository transactionRepository,
            UserCategoryMappingRepository mappingRepository,
            TransactionCategorizationService categorizationService) {
        this.transactionRepository = transactionRepository;
        this.mappingRepository = mappingRepository;
        this.categorizationService = categorizationService;
    }

    /**
     * Detect uncategorized recurring patterns for a user.
     * Returns patterns that appear multiple times but are categorized as "OTHER".
     */
    public List<UncategorizedPatternDto> detectUncategorizedPatterns(UUID profileId, int minOccurrences) {
        List<TransactionEntity> transactions = transactionRepository
                .findByProfileIdOrderByTxnDateDescCreatedAtDesc(profileId);

        // Filter to only uncategorized (OTHER) transactions
        List<TransactionEntity> uncategorized = transactions.stream()
                .filter(t -> "OTHER".equals(t.getCategory()) || t.getCategory() == null)
                .collect(Collectors.toList());

        List<UncategorizedPatternDto> patterns = new ArrayList<>();

        // 1. Detect mobile number patterns
        patterns.addAll(detectMobileNumberPatterns(uncategorized, profileId, minOccurrences));

        // 2. Detect UPI ID patterns
        patterns.addAll(detectUpiIdPatterns(uncategorized, profileId, minOccurrences));

        // 3. Detect counterparty name patterns
        patterns.addAll(detectCounterpartyPatterns(uncategorized, profileId, minOccurrences));

        // Sort by transaction count (most frequent first)
        patterns.sort((a, b) -> Integer.compare(b.getTransactionCount(), a.getTransactionCount()));

        return patterns;
    }

    private List<UncategorizedPatternDto> detectMobileNumberPatterns(
            List<TransactionEntity> transactions, UUID profileId, int minOccurrences) {
        
        Map<String, List<TransactionEntity>> byMobile = new HashMap<>();

        for (TransactionEntity txn : transactions) {
            String mobile = categorizationService.extractMobileNumber(buildSearchText(txn));
            if (mobile != null) {
                byMobile.computeIfAbsent(mobile, k -> new ArrayList<>()).add(txn);
            }
        }

        return byMobile.entrySet().stream()
                .filter(e -> e.getValue().size() >= minOccurrences)
                .filter(e -> !mappingRepository.existsByProfileIdAndPatternTypeAndPatternValue(
                        profileId, PatternType.MOBILE_NUMBER, e.getKey()))
                .map(e -> createPatternDto(PatternType.MOBILE_NUMBER, e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<UncategorizedPatternDto> detectUpiIdPatterns(
            List<TransactionEntity> transactions, UUID profileId, int minOccurrences) {
        
        Map<String, List<TransactionEntity>> byUpiId = new HashMap<>();

        for (TransactionEntity txn : transactions) {
            String upiId = categorizationService.extractUpiId(buildSearchText(txn));
            if (upiId != null && !upiId.matches("\\d+@.*")) { // Exclude mobile@upi (already covered)
                byUpiId.computeIfAbsent(upiId.toLowerCase(), k -> new ArrayList<>()).add(txn);
            }
        }

        return byUpiId.entrySet().stream()
                .filter(e -> e.getValue().size() >= minOccurrences)
                .filter(e -> !mappingRepository.existsByProfileIdAndPatternTypeAndPatternValue(
                        profileId, PatternType.UPI_ID, e.getKey()))
                .map(e -> createPatternDto(PatternType.UPI_ID, e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<UncategorizedPatternDto> detectCounterpartyPatterns(
            List<TransactionEntity> transactions, UUID profileId, int minOccurrences) {
        
        Map<String, List<TransactionEntity>> byCounterparty = new HashMap<>();

        for (TransactionEntity txn : transactions) {
            String counterparty = txn.getCounterpartyName();
            if (counterparty != null && !counterparty.isBlank()) {
                byCounterparty.computeIfAbsent(counterparty.toLowerCase().trim(), k -> new ArrayList<>()).add(txn);
            }
        }

        return byCounterparty.entrySet().stream()
                .filter(e -> e.getValue().size() >= minOccurrences)
                .filter(e -> !mappingRepository.existsByProfileIdAndPatternTypeAndPatternValue(
                        profileId, PatternType.COUNTERPARTY_NAME, e.getKey()))
                .map(e -> createPatternDto(PatternType.COUNTERPARTY_NAME, e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private UncategorizedPatternDto createPatternDto(
            PatternType type, String value, List<TransactionEntity> transactions) {
        
        UncategorizedPatternDto dto = new UncategorizedPatternDto();
        dto.setPatternType(type);
        dto.setPatternValue(value);
        dto.setTransactionCount(transactions.size());
        dto.setTotalAmount(transactions.stream()
                .mapToDouble(t -> t.getAmount() != null ? Math.abs(t.getAmount()) / 100.0 : 0)
                .sum());

        // Use first transaction for sample
        TransactionEntity sample = transactions.get(0);
        dto.setSampleDescription(sample.getDescription());
        dto.setSuggestedDisplayName(generateDisplayName(type, value, sample));
        dto.setSuggestedCategory(suggestCategory(transactions));

        return dto;
    }

    private String generateDisplayName(PatternType type, String value, TransactionEntity sample) {
        switch (type) {
            case MOBILE_NUMBER:
                return sample.getCounterpartyName() != null ? 
                        sample.getCounterpartyName() : "Mobile: " + value;
            case UPI_ID:
                return value.split("@")[0]; // Use username part
            case COUNTERPARTY_NAME:
                return value.substring(0, 1).toUpperCase() + value.substring(1);
            default:
                return value;
        }
    }

    private String suggestCategory(List<TransactionEntity> transactions) {
        // Simple heuristic: if all amounts are similar, might be subscription
        if (transactions.size() >= 2) {
            long firstAmount = transactions.get(0).getAmount() != null ? transactions.get(0).getAmount() : 0;
            boolean allSameAmount = transactions.stream()
                    .allMatch(t -> t.getAmount() != null && Math.abs(t.getAmount() - firstAmount) < 100);
            if (allSameAmount) {
                return "SUBSCRIPTION";
            }
        }
        return "TRANSFER"; // Default suggestion for person-to-person
    }

    private String buildSearchText(TransactionEntity txn) {
        StringBuilder sb = new StringBuilder();
        if (txn.getDescription() != null) sb.append(txn.getDescription()).append(" ");
        if (txn.getMerchant() != null) sb.append(txn.getMerchant()).append(" ");
        if (txn.getCounterpartyName() != null) sb.append(txn.getCounterpartyName()).append(" ");
        if (txn.getRawText() != null) sb.append(txn.getRawText());
        return sb.toString().trim();
    }
}

