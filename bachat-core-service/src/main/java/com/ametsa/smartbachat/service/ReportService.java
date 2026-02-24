package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.reports.*;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.entity.BudgetCategory;
import com.ametsa.smartbachat.entity.UserBudget;
import com.ametsa.smartbachat.repository.*;
import com.ametsa.smartbachat.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final long PAISA_MULTIPLIER = 100L;

    private final TransactionRepository transactionRepository;
    private final UserBudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final BillReminderRepository billReminderRepository;
    private final SecurityUtils securityUtils;

    public ReportService(
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
     * Generate monthly summary report.
     */
    public MonthlyReportResponse getMonthlyReport(String month) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        YearMonth ym = YearMonth.parse(month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        log.info("Generating monthly report for {} (profile: {})", month, profileId);

        // Get transactions for the month
        List<TransactionEntity> transactions = transactionRepository
                .findByProfileIdAndTxnDateBetweenOrderByTxnDateDesc(profileId, startDate, endDate);

        // Separate income and expenses
        List<TransactionEntity> incomes = transactions.stream()
                .filter(t -> "CREDIT".equals(t.getDirection()))
                .collect(Collectors.toList());

        List<TransactionEntity> expenses = transactions.stream()
                .filter(t -> "DEBIT".equals(t.getDirection()))
                .collect(Collectors.toList());

        double totalIncome = sumAmounts(incomes);
        double totalExpenses = sumAmounts(expenses);
        double netSavings = totalIncome - totalExpenses;
        double savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0;

        // Category breakdowns
        List<CategoryBreakdown> incomeByCategory = getCategoryBreakdown(incomes, totalIncome, totalIncome);
        List<CategoryBreakdown> expensesByCategory = getCategoryBreakdown(expenses, totalExpenses, totalIncome);

        // Previous month comparison
        YearMonth prevYm = ym.minusMonths(1);
        LocalDate prevStart = prevYm.atDay(1);
        LocalDate prevEnd = prevYm.atEndOfMonth();

        Long prevIncomePaisa = transactionRepository.sumCreditsByProfileAndDateRange(profileId, prevStart, prevEnd);
        Long prevExpensesPaisa = transactionRepository.sumDebitsByProfileAndDateRange(profileId, prevStart, prevEnd);

        double prevIncome = paisaToRupees(prevIncomePaisa);
        double prevExpenses = paisaToRupees(prevExpensesPaisa);

        double incomeChange = prevIncome > 0 ? ((totalIncome - prevIncome) / prevIncome) * 100 : 0;
        double expenseChange = prevExpenses > 0 ? ((totalExpenses - prevExpenses) / prevExpenses) * 100 : 0;

        // Top transactions
        List<TopTransaction> topExpenses = getTopTransactions(expenses, 5);
        List<TopTransaction> topIncomes = getTopTransactions(incomes, 5);

        // Budget status
        Optional<UserBudget> budgetOpt = budgetRepository.findByProfileIdAndBudgetMonth(profileId, month);
        boolean hasBudget = budgetOpt.isPresent();
        double budgetUtilization = 0;
        int categoriesOverBudget = 0;

        if (hasBudget) {
            UserBudget budget = budgetOpt.get();
            List<BudgetCategory> categories = budgetCategoryRepository.findByBudgetIdOrderByCategoryAsc(budget.getId());
            budgetUtilization = budget.getSpentPercent();
            categoriesOverBudget = (int) categories.stream().filter(BudgetCategory::isOverBudget).count();
        }

        // Generate insights
        List<String> insights = generateInsights(totalIncome, totalExpenses, prevIncome, prevExpenses, 
                savingsRate, expensesByCategory, hasBudget, categoriesOverBudget);

        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear();

        return MonthlyReportResponse.builder()
                .month(month)
                .monthName(monthName)
                .totalIncome(round(totalIncome))
                .incomeTransactionCount(incomes.size())
                .incomeByCategory(incomeByCategory)
                .totalExpenses(round(totalExpenses))
                .expenseTransactionCount(expenses.size())
                .expensesByCategory(expensesByCategory)
                .netSavings(round(netSavings))
                .savingsRate(round(savingsRate))
                .previousMonthExpenses(round(prevExpenses))
                .expenseChangePercent(round(expenseChange))
                .expenseTrend(getTrend(expenseChange))
                .previousMonthIncome(round(prevIncome))
                .incomeChangePercent(round(incomeChange))
                .incomeTrend(getTrend(incomeChange))
                .topExpenses(topExpenses)
                .topIncomes(topIncomes)
                .hasBudget(hasBudget)
                .budgetUtilization(round(budgetUtilization))
                .categoriesOverBudget(categoriesOverBudget)
                .insights(insights)
                .generatedAt(Instant.now())
                .build();
    }

    private double sumAmounts(List<TransactionEntity> transactions) {
        return transactions.stream()
                .mapToLong(t -> t.getAmount() != null ? Math.abs(t.getAmount()) : 0)
                .sum() / (double) PAISA_MULTIPLIER;
    }

    private Double paisaToRupees(Long paisa) {
        return paisa != null ? paisa / (double) PAISA_MULTIPLIER : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private String getTrend(double changePercent) {
        if (changePercent > 5) return "UP";
        if (changePercent < -5) return "DOWN";
        return "STABLE";
    }

    private List<CategoryBreakdown> getCategoryBreakdown(List<TransactionEntity> transactions,
            double totalAmount, double totalIncome) {
        Map<String, List<TransactionEntity>> byCategory = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCategory() != null ? t.getCategory() : "UNCATEGORIZED"));

        return byCategory.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<TransactionEntity> txns = entry.getValue();
                    double amount = sumAmounts(txns);
                    double percentOfTotal = totalAmount > 0 ? (amount / totalAmount) * 100 : 0;
                    double percentOfIncome = totalIncome > 0 ? (amount / totalIncome) * 100 : 0;

                    return CategoryBreakdown.builder()
                            .category(category)
                            .amount(round(amount))
                            .transactionCount(txns.size())
                            .percentOfTotal(round(percentOfTotal))
                            .percentOfIncome(round(percentOfIncome))
                            .icon(getCategoryIcon(category))
                            .color(getCategoryColor(category))
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getAmount(), a.getAmount()))
                .collect(Collectors.toList());
    }

    private List<TopTransaction> getTopTransactions(List<TransactionEntity> transactions, int limit) {
        return transactions.stream()
                .sorted((a, b) -> Long.compare(
                        Math.abs(b.getAmount() != null ? b.getAmount() : 0),
                        Math.abs(a.getAmount() != null ? a.getAmount() : 0)))
                .limit(limit)
                .map(t -> TopTransaction.builder()
                        .id(t.getId())
                        .date(t.getTxnDate())
                        .amount(paisaToRupees(Math.abs(t.getAmount() != null ? t.getAmount() : 0)))
                        .description(t.getDescription())
                        .category(t.getCategory())
                        .merchant(t.getMerchant())
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> generateInsights(double income, double expenses, double prevIncome,
            double prevExpenses, double savingsRate, List<CategoryBreakdown> expensesByCategory,
            boolean hasBudget, int categoriesOverBudget) {
        List<String> insights = new ArrayList<>();

        // Savings rate insight
        if (savingsRate >= 20) {
            insights.add("Great job! You saved " + round(savingsRate) + "% of your income this month.");
        } else if (savingsRate >= 10) {
            insights.add("You saved " + round(savingsRate) + "% of your income. Try to reach 20% for better financial health.");
        } else if (savingsRate > 0) {
            insights.add("Your savings rate is " + round(savingsRate) + "%. Consider reducing discretionary spending.");
        } else {
            insights.add("You spent more than you earned this month. Review your expenses to find areas to cut back.");
        }

        // Expense trend insight
        if (prevExpenses > 0) {
            double expenseChange = ((expenses - prevExpenses) / prevExpenses) * 100;
            if (expenseChange > 10) {
                insights.add("Your expenses increased by " + round(expenseChange) + "% compared to last month.");
            } else if (expenseChange < -10) {
                insights.add("Well done! You reduced expenses by " + round(Math.abs(expenseChange)) + "% compared to last month.");
            }
        }

        // Top spending category insight
        if (!expensesByCategory.isEmpty()) {
            CategoryBreakdown topCategory = expensesByCategory.get(0);
            insights.add("Your highest spending category was " + topCategory.getCategory() +
                    " at ₹" + topCategory.getAmount() + " (" + topCategory.getPercentOfTotal() + "% of expenses).");
        }

        // Budget insight
        if (hasBudget && categoriesOverBudget > 0) {
            insights.add("You exceeded budget in " + categoriesOverBudget + " categories. Review your budget allocations.");
        }

        return insights;
    }

    private String getCategoryIcon(String category) {
        return switch (category.toUpperCase()) {
            case "FOOD", "RESTAURANT", "DINING" -> "restaurant";
            case "TRANSPORT", "TRAVEL" -> "directions_car";
            case "SHOPPING" -> "shopping_bag";
            case "UTILITIES", "BILLS" -> "bolt";
            case "ENTERTAINMENT" -> "movie";
            case "HEALTHCARE", "MEDICAL" -> "local_hospital";
            case "EDUCATION" -> "school";
            case "RENT", "HOUSING" -> "home";
            case "SALARY", "INCOME" -> "payments";
            case "INVESTMENT" -> "trending_up";
            case "SAVINGS" -> "savings";
            default -> "receipt";
        };
    }

    private String getCategoryColor(String category) {
        return switch (category.toUpperCase()) {
            case "FOOD", "RESTAURANT", "DINING" -> "#E91E63";
            case "TRANSPORT", "TRAVEL" -> "#00BCD4";
            case "SHOPPING" -> "#9C27B0";
            case "UTILITIES", "BILLS" -> "#FFC107";
            case "ENTERTAINMENT" -> "#3F51B5";
            case "HEALTHCARE", "MEDICAL" -> "#F44336";
            case "EDUCATION" -> "#4CAF50";
            case "RENT", "HOUSING" -> "#795548";
            case "SALARY", "INCOME" -> "#4CAF50";
            case "INVESTMENT" -> "#2196F3";
            case "SAVINGS" -> "#FF9800";
            default -> "#607D8B";
        };
    }
}

