package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.TransactionDto;
import com.ametsa.smartbachat.dto.dashboard.*;
import com.ametsa.smartbachat.dto.goals.GoalsSummaryResponse;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.repository.BankAccountRepository;
import com.ametsa.smartbachat.repository.SavingsGoalRepository;
import com.ametsa.smartbachat.repository.TransactionRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for aggregating dashboard data from various sources.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final long PAISA_MULTIPLIER = 100L;

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final GoalService goalService;
    private final SecurityUtils securityUtils;
    private final AiAdvisorClient aiAdvisorClient;

    public DashboardService(
            TransactionRepository transactionRepository,
            BankAccountRepository bankAccountRepository,
            SavingsGoalRepository savingsGoalRepository,
            GoalService goalService,
            SecurityUtils securityUtils,
            AiAdvisorClient aiAdvisorClient) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.goalService = goalService;
        this.securityUtils = securityUtils;
        this.aiAdvisorClient = aiAdvisorClient;
    }

    /**
     * Get aggregated dashboard data for the current user.
     */
    public DashboardResponse getDashboard() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        log.info("Generating dashboard for profile: {}", profileId);

        return DashboardResponse.builder()
                .totalSavings(getTotalSavings(profileId))
                .nudge(generateNudge(profileId))
                .balance(getBalanceSummary(profileId))
                .savingsGoal(getSavingsGoalSummary(profileId))
                .gamification(getGamificationSummary(profileId))
                .forecast(getForecastSummary(profileId))
                .alerts(getAlerts(profileId))
                .recentTransactions(getRecentTransactions(profileId))
                .generatedAt(Instant.now())
                .build();
    }

    private Double getTotalSavings(UUID profileId) {
        Long totalPaisa = savingsGoalRepository.sumCurrentAmountByProfileId(profileId);
        return paisaToRupees(totalPaisa);
    }

    private BalanceSummary getBalanceSummary(UUID profileId) {
        Long totalBalancePaisa = bankAccountRepository.sumBalanceByProfileId(profileId);
        Integer accountCount = bankAccountRepository.countActiveAccountsByProfileId(profileId);
        Instant lastSyncedAt = bankAccountRepository.getLastSyncTimeByProfileId(profileId);

        return BalanceSummary.builder()
                .totalBalance(paisaToRupees(totalBalancePaisa))
                .accountCount(accountCount != null ? accountCount : 0)
                .lastSyncedAt(lastSyncedAt)
                .currency("INR")
                .build();
    }

    private SavingsGoalSummary getSavingsGoalSummary(UUID profileId) {
        GoalsSummaryResponse summary = goalService.getGoalsSummary();

        return SavingsGoalSummary.builder()
                .totalTarget(summary.getTotalTargetAmount())
                .totalSaved(summary.getTotalSavedAmount())
                .progressPercent(summary.getOverallProgressPercent())
                .suggestedMonthlySaving(summary.getTotalSuggestedMonthlySaving())
                .activeGoals(summary.getActiveGoals())
                .goalsOnTrack(summary.getGoalsOnTrack())
                .goalsBehindSchedule(summary.getGoalsBehindSchedule())
                .build();
    }

    private List<TransactionDto> getRecentTransactions(UUID profileId) {
        List<TransactionEntity> transactions = transactionRepository
                .findTop5ByProfileIdOrderByTxnDateDescCreatedAtDesc(profileId);

        return transactions.stream()
                .map(this::toTransactionDto)
                .collect(Collectors.toList());
    }

    private TransactionDto toTransactionDto(TransactionEntity e) {
        return TransactionDto.builder()
                .id(e.getId())
                .statementId(e.getStatementId())
                .profileId(e.getProfileId())
                .txnDate(e.getTxnDate())
                .amount(e.getAmount() != null ? Math.abs(e.getAmount()) / 100.0 : null)
                .direction(e.getDirection())
                .currency(e.getCurrency() != null ? e.getCurrency() : "INR")
                .txnType(e.getTxnType())
                .description(e.getDescription())
                .merchant(e.getMerchant())
                .balance(e.getBalance() != null ? e.getBalance() / 100.0 : null)
                .category(e.getCategory())
                .subCategory(e.getSubCategory())
                .build();
    }

    private Double paisaToRupees(Long paisa) {
        return paisa != null ? paisa / (double) PAISA_MULTIPLIER : 0.0;
    }

    private NudgeMessage generateNudge(UUID profileId) {
        // First check if user has any connected bank accounts
        Integer connectedAccounts = bankAccountRepository.countActiveAccountsByProfileId(profileId);
        boolean hasConnectedAccounts = connectedAccounts != null && connectedAccounts > 0;

        // Calculate savings comparison with last month
        LocalDate today = LocalDate.now();
        LocalDate startOfThisMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate startOfLastMonth = startOfThisMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfThisMonth.minusDays(1);

        Long thisMonthCredits = transactionRepository.sumCreditsByProfileAndDateRange(
                profileId, startOfThisMonth, today);
        Long thisMonthDebits = transactionRepository.sumDebitsByProfileAndDateRange(
                profileId, startOfThisMonth, today);
        Long lastMonthCredits = transactionRepository.sumCreditsByProfileAndDateRange(
                profileId, startOfLastMonth, endOfLastMonth);
        Long lastMonthDebits = transactionRepository.sumDebitsByProfileAndDateRange(
                profileId, startOfLastMonth, endOfLastMonth);

        long thisMonthSavings = (thisMonthCredits != null ? thisMonthCredits : 0)
                - (thisMonthDebits != null ? thisMonthDebits : 0);
        long lastMonthSavings = (lastMonthCredits != null ? lastMonthCredits : 0)
                - (lastMonthDebits != null ? lastMonthDebits : 0);

        // Generate appropriate nudge based on comparison
        if (lastMonthSavings > 0 && thisMonthSavings > lastMonthSavings) {
            double percentIncrease = ((thisMonthSavings - lastMonthSavings) * 100.0) / lastMonthSavings;
            return NudgeMessage.builder()
                    .title("Great job! 🎉")
                    .message(String.format("You're saving %.0f%% more than last month!", percentIncrease))
                    .type("POSITIVE")
                    .build();
        } else if (thisMonthSavings > 0) {
            return NudgeMessage.builder()
                    .title("Keep it up! 💪")
                    .message(String.format("You've saved ₹%.0f this month!", paisaToRupees(thisMonthSavings)))
                    .type("POSITIVE")
                    .build();
        } else if (thisMonthDebits != null && thisMonthDebits > 0) {
            return NudgeMessage.builder()
                    .title("Watch your spending 👀")
                    .message("Your expenses are higher than income this month. Review your transactions.")
                    .type("WARNING")
                    .actionType("REVIEW_SPENDING")
                    .build();
        } else if (hasConnectedAccounts) {
            // User has connected accounts but no transactions this month yet
            // Check last month's performance
            if (lastMonthSavings > 0) {
                return NudgeMessage.builder()
                        .title("Good start! 📊")
                        .message(String.format("Last month you saved ₹%.0f. Keep the momentum going!",
                                paisaToRupees(lastMonthSavings)))
                        .type("POSITIVE")
                        .build();
            } else if (lastMonthSavings < 0) {
                return NudgeMessage.builder()
                        .title("New month, fresh start! 🌟")
                        .message("This is a great opportunity to improve your savings this month.")
                        .type("NEUTRAL")
                        .actionType("SET_BUDGET")
                        .build();
            } else {
                return NudgeMessage.builder()
                        .title("Sync your accounts 🔄")
                        .message("Sync your bank accounts to see your latest transactions.")
                        .type("NEUTRAL")
                        .actionType("SYNC_ACCOUNTS")
                        .build();
            }
        } else {
            return NudgeMessage.builder()
                    .title("Welcome! 👋")
                    .message("Connect your bank account to start tracking your finances.")
                    .type("NEUTRAL")
                    .actionType("CONNECT_BANK")
                    .build();
        }
    }

    private ForecastSummary getForecastSummary(UUID profileId) {
        // Try to get AI-powered forecast first
        String authToken = securityUtils.getCurrentToken();
        if (authToken != null) {
            Optional<ForecastSummary> aiForecast = aiAdvisorClient.getForecast(authToken);
            if (aiForecast.isPresent()) {
                log.info("Using AI-powered forecast (method: {})", aiForecast.get().getForecastMethod());
                return aiForecast.get();
            }
        }

        // Fallback to simple calculation if AI service unavailable
        log.info("AI Advisor unavailable, using simple forecast calculation");
        return getSimpleForecast(profileId);
    }

    /**
     * Simple forecast calculation (fallback when AI service is unavailable).
     */
    private ForecastSummary getSimpleForecast(UUID profileId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate threeMonthsAgo = startOfMonth.minusMonths(3);

        // Get current month data
        Long currentMonthCredits = transactionRepository.sumCreditsByProfileAndDateRange(
                profileId, startOfMonth, today);
        Long currentMonthDebits = transactionRepository.sumDebitsByProfileAndDateRange(
                profileId, startOfMonth, today);

        // Get last 3 months for averages
        Long threeMonthCredits = transactionRepository.sumCreditsByProfileAndDateRange(
                profileId, threeMonthsAgo, startOfMonth.minusDays(1));
        Long threeMonthDebits = transactionRepository.sumDebitsByProfileAndDateRange(
                profileId, threeMonthsAgo, startOfMonth.minusDays(1));

        double avgMonthlyIncome = paisaToRupees(threeMonthCredits) / 3.0;
        double avgMonthlyExpense = paisaToRupees(threeMonthDebits) / 3.0;

        // Project based on days elapsed
        int daysInMonth = today.lengthOfMonth();
        int daysElapsed = today.getDayOfMonth();
        double projectionFactor = (double) daysInMonth / daysElapsed;

        double projectedIncome = paisaToRupees(currentMonthCredits) * projectionFactor;
        double projectedExpense = paisaToRupees(currentMonthDebits) * projectionFactor;
        double projectedSavings = projectedIncome - projectedExpense;

        // Determine trend
        String trend = "STABLE";
        double changePercent = 0;
        if (avgMonthlyExpense > 0) {
            changePercent = ((projectedExpense - avgMonthlyExpense) / avgMonthlyExpense) * 100;
            if (changePercent > 10) trend = "UP";
            else if (changePercent < -10) trend = "DOWN";
        }

        double savingsRate = projectedIncome > 0 ? (projectedSavings / projectedIncome) * 100 : 0;

        return ForecastSummary.builder()
                .projectedExpense(Math.round(projectedExpense * 100) / 100.0)
                .projectedIncome(Math.round(projectedIncome * 100) / 100.0)
                .projectedSavings(Math.round(projectedSavings * 100) / 100.0)
                .trend(trend)
                .changePercent(Math.round(changePercent * 10) / 10.0)
                .avgMonthlyExpense(Math.round(avgMonthlyExpense * 100) / 100.0)
                .avgMonthlyIncome(Math.round(avgMonthlyIncome * 100) / 100.0)
                .savingsRate(Math.round(savingsRate * 10) / 10.0)
                .forecastMethod("SIMPLE_PROJECTION")
                .confidenceScore(0.5)
                .insights(List.of("Using simple projection. Start AI Advisor service for better predictions."))
                .build();
    }

    private GamificationSummary getGamificationSummary(UUID profileId) {
        // Placeholder - will be implemented in Phase 2
        return GamificationSummary.builder()
                .currentStreak(0)
                .longestStreak(0)
                .recentBadges(new ArrayList<>())
                .activeChallenges(new ArrayList<>())
                .level(1)
                .xp(0)
                .totalBadges(0)
                .build();
    }

    private List<AlertItem> getAlerts(UUID profileId) {
        // Placeholder - will be implemented in Phase 3
        // For now, check for expiring consents
        List<AlertItem> alerts = new ArrayList<>();

        // Check goals behind schedule
        GoalsSummaryResponse goalsSummary = goalService.getGoalsSummary();
        if (goalsSummary.getGoalsBehindSchedule() != null && goalsSummary.getGoalsBehindSchedule() > 0) {
            alerts.add(AlertItem.builder()
                    .id("goals-behind")
                    .type("GOAL_REMINDER")
                    .title("Goals Need Attention")
                    .message(String.format("%d goal(s) are behind schedule. Consider adding contributions.",
                            goalsSummary.getGoalsBehindSchedule()))
                    .priority("MEDIUM")
                    .isRead(false)
                    .actionType("VIEW_GOALS")
                    .build());
        }

        return alerts;
    }
}

