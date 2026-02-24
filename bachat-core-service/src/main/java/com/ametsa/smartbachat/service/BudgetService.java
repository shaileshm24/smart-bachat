package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.budget.*;
import com.ametsa.smartbachat.entity.BudgetCategory;
import com.ametsa.smartbachat.entity.BudgetTemplate;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.entity.UserBudget;
import com.ametsa.smartbachat.repository.BudgetCategoryRepository;
import com.ametsa.smartbachat.repository.BudgetTemplateRepository;
import com.ametsa.smartbachat.repository.TransactionRepository;
import com.ametsa.smartbachat.repository.UserBudgetRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);
    private static final long PAISA_MULTIPLIER = 100L;

    private final UserBudgetRepository budgetRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final BudgetTemplateRepository templateRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public BudgetService(
            UserBudgetRepository budgetRepository,
            BudgetCategoryRepository categoryRepository,
            BudgetTemplateRepository templateRepository,
            TransactionRepository transactionRepository,
            SecurityUtils securityUtils,
            ObjectMapper objectMapper) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.templateRepository = templateRepository;
        this.transactionRepository = transactionRepository;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all available budget templates.
     */
    public List<BudgetTemplateDto> getTemplates() {
        return templateRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::mapToTemplateDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new budget for a month.
     */
    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();

        // Check if budget already exists for this month
        if (budgetRepository.existsByProfileIdAndBudgetMonth(profileId, request.getBudgetMonth())) {
            throw new RuntimeException("Budget already exists for " + request.getBudgetMonth());
        }

        UserBudget budget = new UserBudget();
        budget.setProfileId(profileId);
        budget.setBudgetMonth(request.getBudgetMonth());
        budget.setTotalIncome(rupeesToPaisa(request.getTotalIncome()));
        budget.setNotes(request.getNotes());

        List<BudgetCategoryRequest> categoryRequests;

        // If template is provided, use its allocations
        if (request.getTemplateId() != null) {
            BudgetTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Template not found"));
            budget.setTemplateId(template.getId());
            categoryRequests = parseTemplateAllocations(template, request.getTotalIncome());
        } else if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            categoryRequests = request.getCategories();
        } else {
            throw new RuntimeException("Either templateId or categories must be provided");
        }

        budget = budgetRepository.save(budget);
        log.info("Created budget for month: {}", request.getBudgetMonth());

        // Create category allocations
        long totalBudget = createCategories(budget.getId(), categoryRequests, request.getTotalIncome());
        budget.setTotalBudget(totalBudget);
        budget = budgetRepository.save(budget);

        // Update spent amounts from transactions
        updateSpentAmounts(budget);

        return getBudgetResponse(budget);
    }

    // Continued in next part...
    private Long rupeesToPaisa(Double rupees) {
        return rupees != null ? Math.round(rupees * PAISA_MULTIPLIER) : 0L;
    }

    private Double paisaToRupees(Long paisa) {
        return paisa != null ? paisa / (double) PAISA_MULTIPLIER : 0.0;
    }

    private BudgetTemplateDto mapToTemplateDto(BudgetTemplate template) {
        List<CategoryAllocationDto> allocations = new ArrayList<>();
        if (template.getCategoryAllocations() != null) {
            try {
                allocations = objectMapper.readValue(
                        template.getCategoryAllocations(),
                        new TypeReference<List<CategoryAllocationDto>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse template allocations: {}", e.getMessage());
            }
        }

        return BudgetTemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .templateType(template.getTemplateType())
                .description(template.getDescription())
                .categoryAllocations(allocations)
                .isSystem(template.getIsSystem())
                .icon(template.getIcon())
                .createdAt(template.getCreatedAt())
                .build();
    }

    private List<BudgetCategoryRequest> parseTemplateAllocations(BudgetTemplate template, Double totalIncome) {
        List<CategoryAllocationDto> allocations = new ArrayList<>();
        if (template.getCategoryAllocations() != null) {
            try {
                allocations = objectMapper.readValue(
                        template.getCategoryAllocations(),
                        new TypeReference<List<CategoryAllocationDto>>() {});
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse template allocations");
            }
        }

        return allocations.stream()
                .map(a -> BudgetCategoryRequest.builder()
                        .category(a.getCategory())
                        .percentage(a.getPercentage())
                        .budgetLimit(totalIncome * a.getPercentage() / 100)
                        .icon(a.getIcon())
                        .color(a.getColor())
                        .build())
                .collect(Collectors.toList());
    }

    private long createCategories(UUID budgetId, List<BudgetCategoryRequest> requests, Double totalIncome) {
        long totalBudget = 0;
        for (BudgetCategoryRequest req : requests) {
            BudgetCategory category = new BudgetCategory();
            category.setBudgetId(budgetId);
            category.setCategory(req.getCategory());
            category.setPercentage(req.getPercentage());
            category.setIcon(req.getIcon());
            category.setColor(req.getColor());

            // Calculate budget limit
            long limit;
            if (req.getBudgetLimit() != null) {
                limit = rupeesToPaisa(req.getBudgetLimit());
            } else if (req.getPercentage() != null && totalIncome != null) {
                limit = rupeesToPaisa(totalIncome * req.getPercentage() / 100);
            } else {
                limit = 0L;
            }
            category.setBudgetLimit(limit);
            totalBudget += limit;

            categoryRepository.save(category);
        }
        return totalBudget;
    }

    private void updateSpentAmounts(UserBudget budget) {
        YearMonth ym = YearMonth.parse(budget.getBudgetMonth());
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<TransactionEntity> transactions = transactionRepository
                .findByProfileIdAndTxnDateBetweenOrderByTxnDateDesc(
                        budget.getProfileId(), startDate, endDate);

        // Group expenses by category
        Map<String, Long> spentByCategory = transactions.stream()
                .filter(t -> "DEBIT".equals(t.getDirection()) && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        TransactionEntity::getCategory,
                        Collectors.summingLong(TransactionEntity::getAmount)));

        // Update each category's spent amount
        List<BudgetCategory> categories = categoryRepository.findByBudgetIdOrderByCategoryAsc(budget.getId());
        long totalSpent = 0;
        for (BudgetCategory cat : categories) {
            Long spent = spentByCategory.getOrDefault(cat.getCategory(), 0L);
            cat.setSpentAmount(spent);
            cat.setUpdatedAt(Instant.now());
            categoryRepository.save(cat);
            totalSpent += spent;
        }

        budget.setTotalSpent(totalSpent);
        budget.setUpdatedAt(Instant.now());
        budgetRepository.save(budget);
    }

    /**
     * Get budget for a specific month.
     */
    public BudgetResponse getBudget(String budgetMonth) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        UserBudget budget = budgetRepository.findByProfileIdAndBudgetMonth(profileId, budgetMonth)
                .orElseThrow(() -> new RuntimeException("Budget not found for " + budgetMonth));

        // Refresh spent amounts
        updateSpentAmounts(budget);
        return getBudgetResponse(budget);
    }

    /**
     * Get current month's budget.
     */
    public BudgetResponse getCurrentBudget() {
        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return getBudget(currentMonth);
    }

    /**
     * Get all budgets for the user.
     */
    public List<BudgetResponse> getAllBudgets() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        return budgetRepository.findByProfileIdOrderByBudgetMonthDesc(profileId)
                .stream()
                .map(this::getBudgetResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an existing budget.
     */
    @Transactional
    public BudgetResponse updateBudget(UUID budgetId, UpdateBudgetRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        UserBudget budget = budgetRepository.findByIdAndProfileId(budgetId, profileId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        if (request.getTotalIncome() != null) {
            budget.setTotalIncome(rupeesToPaisa(request.getTotalIncome()));
        }
        if (request.getStatus() != null) {
            budget.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }

        // Update categories if provided
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            categoryRepository.deleteByBudgetId(budgetId);
            Double totalIncome = paisaToRupees(budget.getTotalIncome());
            long totalBudget = createCategories(budgetId, request.getCategories(), totalIncome);
            budget.setTotalBudget(totalBudget);
        }

        budget.setUpdatedAt(Instant.now());
        budget = budgetRepository.save(budget);

        updateSpentAmounts(budget);
        return getBudgetResponse(budget);
    }

    /**
     * Delete a budget.
     */
    @Transactional
    public void deleteBudget(UUID budgetId) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        UserBudget budget = budgetRepository.findByIdAndProfileId(budgetId, profileId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        categoryRepository.deleteByBudgetId(budgetId);
        budgetRepository.delete(budget);
        log.info("Deleted budget: {}", budgetId);
    }

    private BudgetResponse getBudgetResponse(UserBudget budget) {
        List<BudgetCategory> categories = categoryRepository.findByBudgetIdOrderByCategoryAsc(budget.getId());

        List<BudgetCategoryDto> categoryDtos = categories.stream()
                .map(this::mapToCategoryDto)
                .collect(Collectors.toList());

        int overBudget = (int) categories.stream().filter(BudgetCategory::isOverBudget).count();

        String templateName = null;
        if (budget.getTemplateId() != null) {
            templateName = templateRepository.findById(budget.getTemplateId())
                    .map(BudgetTemplate::getName)
                    .orElse(null);
        }

        return BudgetResponse.builder()
                .id(budget.getId())
                .budgetMonth(budget.getBudgetMonth())
                .templateId(budget.getTemplateId())
                .templateName(templateName)
                .totalIncome(paisaToRupees(budget.getTotalIncome()))
                .totalBudget(paisaToRupees(budget.getTotalBudget()))
                .totalSpent(paisaToRupees(budget.getTotalSpent()))
                .remainingBudget(paisaToRupees(budget.getRemainingBudget()))
                .spentPercent(budget.getSpentPercent())
                .status(budget.getStatus())
                .notes(budget.getNotes())
                .categories(categoryDtos)
                .categoriesOnTrack(categories.size() - overBudget)
                .categoriesOverBudget(overBudget)
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }

    private BudgetCategoryDto mapToCategoryDto(BudgetCategory category) {
        return BudgetCategoryDto.builder()
                .id(category.getId())
                .category(category.getCategory())
                .budgetLimit(paisaToRupees(category.getBudgetLimit()))
                .percentage(category.getPercentage())
                .spentAmount(paisaToRupees(category.getSpentAmount()))
                .remainingAmount(paisaToRupees(category.getRemainingAmount()))
                .spentPercent(category.getSpentPercent())
                .isOverBudget(category.isOverBudget())
                .icon(category.getIcon())
                .color(category.getColor())
                .build();
    }
}

