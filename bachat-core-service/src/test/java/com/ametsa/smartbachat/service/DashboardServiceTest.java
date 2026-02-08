package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.dashboard.*;
import com.ametsa.smartbachat.dto.goals.GoalsSummaryResponse;
import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.repository.BankAccountRepository;
import com.ametsa.smartbachat.repository.SavingsGoalRepository;
import com.ametsa.smartbachat.repository.TransactionRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private SavingsGoalRepository savingsGoalRepository;
    @Mock private GoalService goalService;
    @Mock private SecurityUtils securityUtils;
    @Mock private AiAdvisorClient aiAdvisorClient;

    private DashboardService dashboardService;
    private UUID testProfileId;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                transactionRepository, bankAccountRepository, savingsGoalRepository,
                goalService, securityUtils, aiAdvisorClient);
        testProfileId = UUID.randomUUID();
        lenient().when(securityUtils.requireCurrentProfileId()).thenReturn(testProfileId);
        // Default: AI service unavailable, use fallback
        lenient().when(securityUtils.getCurrentToken()).thenReturn(null);
    }

    @Nested
    @DisplayName("Get Dashboard Tests")
    class GetDashboardTests {

        @BeforeEach
        void setUpMocks() {
            // Default mocks for a complete dashboard
            when(savingsGoalRepository.sumCurrentAmountByProfileId(testProfileId)).thenReturn(1250000L);
            when(bankAccountRepository.sumBalanceByProfileId(testProfileId)).thenReturn(4580000L);
            when(bankAccountRepository.countActiveAccountsByProfileId(testProfileId)).thenReturn(2);
            when(bankAccountRepository.getLastSyncTimeByProfileId(testProfileId)).thenReturn(Instant.now());
            when(transactionRepository.findTop5ByProfileIdOrderByTxnDateDescCreatedAtDesc(testProfileId))
                    .thenReturn(Collections.emptyList());
            
            GoalsSummaryResponse goalsSummary = new GoalsSummaryResponse();
            goalsSummary.setTotalTargetAmount(50000.0);
            goalsSummary.setTotalSavedAmount(12500.0);
            goalsSummary.setOverallProgressPercent(25.0);
            goalsSummary.setTotalSuggestedMonthlySaving(6250.0);
            goalsSummary.setActiveGoals(3);
            goalsSummary.setGoalsOnTrack(2);
            goalsSummary.setGoalsBehindSchedule(1);
            when(goalService.getGoalsSummary()).thenReturn(goalsSummary);
        }

        @Test
        void shouldReturnCompleteDashboard() {
            DashboardResponse dashboard = dashboardService.getDashboard();

            assertNotNull(dashboard);
            assertNotNull(dashboard.getTotalSavings());
            assertNotNull(dashboard.getNudge());
            assertNotNull(dashboard.getBalance());
            assertNotNull(dashboard.getSavingsGoal());
            assertNotNull(dashboard.getGamification());
            assertNotNull(dashboard.getForecast());
            assertNotNull(dashboard.getAlerts());
            assertNotNull(dashboard.getRecentTransactions());
            assertNotNull(dashboard.getGeneratedAt());
        }

        @Test
        void shouldCalculateTotalSavingsCorrectly() {
            DashboardResponse dashboard = dashboardService.getDashboard();

            // 1250000 paisa = 12500 rupees
            assertEquals(12500.0, dashboard.getTotalSavings());
        }
    }

    @Nested
    @DisplayName("Balance Summary Tests")
    class BalanceSummaryTests {

        @BeforeEach
        void setUpDefaultMocks() {
            setupDefaultGoalMocks();
        }

        @Test
        void shouldCalculateBalanceSummaryCorrectly() {
            when(bankAccountRepository.sumBalanceByProfileId(testProfileId)).thenReturn(10000000L);
            when(bankAccountRepository.countActiveAccountsByProfileId(testProfileId)).thenReturn(3);
            Instant syncTime = Instant.now();
            when(bankAccountRepository.getLastSyncTimeByProfileId(testProfileId)).thenReturn(syncTime);

            DashboardResponse dashboard = dashboardService.getDashboard();

            assertEquals(100000.0, dashboard.getBalance().getTotalBalance());
            assertEquals(3, dashboard.getBalance().getAccountCount());
            assertEquals(syncTime, dashboard.getBalance().getLastSyncedAt());
            assertEquals("INR", dashboard.getBalance().getCurrency());
        }

        @Test
        void shouldHandleNoConnectedAccounts() {
            when(bankAccountRepository.sumBalanceByProfileId(testProfileId)).thenReturn(0L);
            when(bankAccountRepository.countActiveAccountsByProfileId(testProfileId)).thenReturn(0);
            when(bankAccountRepository.getLastSyncTimeByProfileId(testProfileId)).thenReturn(null);

            DashboardResponse dashboard = dashboardService.getDashboard();

            assertEquals(0.0, dashboard.getBalance().getTotalBalance());
            assertEquals(0, dashboard.getBalance().getAccountCount());
            assertNull(dashboard.getBalance().getLastSyncedAt());
        }
    }

    @Nested
    @DisplayName("Savings Goal Summary Tests")
    class SavingsGoalSummaryTests {

        @BeforeEach
        void setUpDefaultMocks() {
            setupDefaultBankMocks();
        }

        @Test
        void shouldReturnGoalsSummaryFromGoalService() {
            GoalsSummaryResponse goalsSummary = new GoalsSummaryResponse();
            goalsSummary.setTotalTargetAmount(100000.0);
            goalsSummary.setTotalSavedAmount(25000.0);
            goalsSummary.setOverallProgressPercent(25.0);
            goalsSummary.setTotalSuggestedMonthlySaving(12500.0);
            goalsSummary.setActiveGoals(5);
            goalsSummary.setGoalsOnTrack(3);
            goalsSummary.setGoalsBehindSchedule(2);
            when(goalService.getGoalsSummary()).thenReturn(goalsSummary);

            DashboardResponse dashboard = dashboardService.getDashboard();

            SavingsGoalSummary summary = dashboard.getSavingsGoal();
            assertEquals(100000.0, summary.getTotalTarget());
            assertEquals(25000.0, summary.getTotalSaved());
            assertEquals(25.0, summary.getProgressPercent());
            assertEquals(12500.0, summary.getSuggestedMonthlySaving());
            assertEquals(5, summary.getActiveGoals());
            assertEquals(3, summary.getGoalsOnTrack());
            assertEquals(2, summary.getGoalsBehindSchedule());
        }
    }

    @Nested
    @DisplayName("Nudge Message Tests")
    class NudgeMessageTests {

        @BeforeEach
        void setUpDefaultMocks() {
            setupDefaultBankMocks();
            setupDefaultGoalMocks();
        }

        @Test
        void shouldGeneratePositiveNudgeWhenSavingsIncreased() {
            LocalDate today = LocalDate.now();
            LocalDate startOfThisMonth = today.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate startOfLastMonth = startOfThisMonth.minusMonths(1);
            LocalDate endOfLastMonth = startOfThisMonth.minusDays(1);

            // This month: income 100000, expense 60000 = savings 40000
            when(transactionRepository.sumCreditsByProfileAndDateRange(eq(testProfileId), eq(startOfThisMonth), eq(today)))
                    .thenReturn(10000000L);
            when(transactionRepository.sumDebitsByProfileAndDateRange(eq(testProfileId), eq(startOfThisMonth), eq(today)))
                    .thenReturn(6000000L);
            // Last month: income 100000, expense 80000 = savings 20000
            when(transactionRepository.sumCreditsByProfileAndDateRange(eq(testProfileId), eq(startOfLastMonth), eq(endOfLastMonth)))
                    .thenReturn(10000000L);
            when(transactionRepository.sumDebitsByProfileAndDateRange(eq(testProfileId), eq(startOfLastMonth), eq(endOfLastMonth)))
                    .thenReturn(8000000L);

            DashboardResponse dashboard = dashboardService.getDashboard();

            NudgeMessage nudge = dashboard.getNudge();
            assertEquals("POSITIVE", nudge.getType());
            assertTrue(nudge.getTitle().contains("Great job"));
        }

        @Test
        void shouldGenerateWarningNudgeWhenExpensesExceedIncome() {
            // This month: income 50000, expense 70000 = negative savings
            // Use lenient any() matcher to handle all date range calls
            lenient().when(transactionRepository.sumCreditsByProfileAndDateRange(any(), any(), any()))
                    .thenReturn(5000000L);
            lenient().when(transactionRepository.sumDebitsByProfileAndDateRange(any(), any(), any()))
                    .thenReturn(7000000L);

            DashboardResponse dashboard = dashboardService.getDashboard();

            NudgeMessage nudge = dashboard.getNudge();
            assertEquals("WARNING", nudge.getType());
            assertEquals("REVIEW_SPENDING", nudge.getActionType());
        }

        @Test
        void shouldGenerateWelcomeNudgeForNewUsers() {
            // No transactions at all and no connected accounts
            when(bankAccountRepository.countActiveAccountsByProfileId(testProfileId)).thenReturn(0);
            when(transactionRepository.sumCreditsByProfileAndDateRange(any(), any(), any())).thenReturn(0L);
            when(transactionRepository.sumDebitsByProfileAndDateRange(any(), any(), any())).thenReturn(0L);

            DashboardResponse dashboard = dashboardService.getDashboard();

            NudgeMessage nudge = dashboard.getNudge();
            assertEquals("NEUTRAL", nudge.getType());
            assertEquals("CONNECT_BANK", nudge.getActionType());
        }

        @Test
        void shouldGenerateSyncNudgeWhenAccountsConnectedButNoTransactions() {
            // User has connected accounts but no transactions at all
            when(bankAccountRepository.countActiveAccountsByProfileId(testProfileId)).thenReturn(2);
            when(transactionRepository.sumCreditsByProfileAndDateRange(any(), any(), any())).thenReturn(0L);
            when(transactionRepository.sumDebitsByProfileAndDateRange(any(), any(), any())).thenReturn(0L);

            DashboardResponse dashboard = dashboardService.getDashboard();

            NudgeMessage nudge = dashboard.getNudge();
            assertEquals("NEUTRAL", nudge.getType());
            assertEquals("SYNC_ACCOUNTS", nudge.getActionType());
            assertTrue(nudge.getMessage().contains("Sync"));
        }

        @Test
        void shouldGeneratePositiveNudgeWhenAccountsConnectedWithLastMonthSavings() {
            LocalDate today = LocalDate.now();
            LocalDate startOfThisMonth = today.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate startOfLastMonth = startOfThisMonth.minusMonths(1);
            LocalDate endOfLastMonth = startOfThisMonth.minusDays(1);

            // User has connected accounts, no transactions this month, but had savings last month
            when(bankAccountRepository.countActiveAccountsByProfileId(testProfileId)).thenReturn(2);
            // This month: no transactions
            when(transactionRepository.sumCreditsByProfileAndDateRange(eq(testProfileId), eq(startOfThisMonth), eq(today)))
                    .thenReturn(0L);
            when(transactionRepository.sumDebitsByProfileAndDateRange(eq(testProfileId), eq(startOfThisMonth), eq(today)))
                    .thenReturn(0L);
            // Last month: income 100000, expense 60000 = savings 40000
            when(transactionRepository.sumCreditsByProfileAndDateRange(eq(testProfileId), eq(startOfLastMonth), eq(endOfLastMonth)))
                    .thenReturn(10000000L);
            when(transactionRepository.sumDebitsByProfileAndDateRange(eq(testProfileId), eq(startOfLastMonth), eq(endOfLastMonth)))
                    .thenReturn(6000000L);

            DashboardResponse dashboard = dashboardService.getDashboard();

            NudgeMessage nudge = dashboard.getNudge();
            assertEquals("POSITIVE", nudge.getType());
            assertTrue(nudge.getMessage().contains("Last month you saved"));
        }
    }

    @Nested
    @DisplayName("Recent Transactions Tests")
    class RecentTransactionsTests {

        @BeforeEach
        void setUpDefaultMocks() {
            setupDefaultBankMocks();
            setupDefaultGoalMocks();
        }

        @Test
        void shouldReturnRecentTransactions() {
            List<TransactionEntity> transactions = Arrays.asList(
                    createTransaction("Swiggy", 45000L, "DEBIT", "FOOD"),
                    createTransaction("Salary", 7500000L, "CREDIT", "SALARY"),
                    createTransaction("Amazon", 250000L, "DEBIT", "SHOPPING")
            );
            when(transactionRepository.findTop5ByProfileIdOrderByTxnDateDescCreatedAtDesc(testProfileId))
                    .thenReturn(transactions);

            DashboardResponse dashboard = dashboardService.getDashboard();

            assertEquals(3, dashboard.getRecentTransactions().size());
            assertEquals(450.0, dashboard.getRecentTransactions().get(0).getAmount());
            assertEquals("DEBIT", dashboard.getRecentTransactions().get(0).getDirection());
            assertEquals("FOOD", dashboard.getRecentTransactions().get(0).getCategory());
        }

        @Test
        void shouldHandleEmptyTransactions() {
            when(transactionRepository.findTop5ByProfileIdOrderByTxnDateDescCreatedAtDesc(testProfileId))
                    .thenReturn(Collections.emptyList());

            DashboardResponse dashboard = dashboardService.getDashboard();

            assertNotNull(dashboard.getRecentTransactions());
            assertTrue(dashboard.getRecentTransactions().isEmpty());
        }
    }

    @Nested
    @DisplayName("Forecast Summary Tests")
    class ForecastSummaryTests {

        @BeforeEach
        void setUpDefaultMocks() {
            setupDefaultBankMocks();
            setupDefaultGoalMocks();
        }

        @Test
        void shouldCalculateForecastBasedOnCurrentMonth() {
            // Use lenient any() matcher to handle all date range calls
            // The service makes multiple calls with different date ranges
            lenient().when(transactionRepository.sumCreditsByProfileAndDateRange(any(), any(), any()))
                    .thenReturn(5000000L); // 50000 rupees
            lenient().when(transactionRepository.sumDebitsByProfileAndDateRange(any(), any(), any()))
                    .thenReturn(3000000L); // 30000 rupees

            DashboardResponse dashboard = dashboardService.getDashboard();

            ForecastSummary forecast = dashboard.getForecast();
            assertNotNull(forecast.getProjectedExpense());
            assertNotNull(forecast.getProjectedIncome());
            assertNotNull(forecast.getProjectedSavings());
            assertNotNull(forecast.getTrend());
            assertNotNull(forecast.getSavingsRate());
        }

        @Test
        void shouldUseAiForecastWhenAvailable() {
            // Setup AI forecast response
            ForecastSummary aiForecast = ForecastSummary.builder()
                    .projectedIncome(150000.0)
                    .projectedExpense(100000.0)
                    .projectedSavings(50000.0)
                    .trend("UP")
                    .changePercent(5.5)
                    .avgMonthlyIncome(145000.0)
                    .avgMonthlyExpense(98000.0)
                    .savingsRate(33.3)
                    .confidenceScore(0.85)
                    .forecastMethod("BLENDED_PROJECTION")
                    .insights(List.of("Your income is trending upward", "Expenses are stable"))
                    .build();

            when(securityUtils.getCurrentToken()).thenReturn("test-jwt-token");
            when(aiAdvisorClient.getForecast("test-jwt-token")).thenReturn(Optional.of(aiForecast));

            DashboardResponse dashboard = dashboardService.getDashboard();

            ForecastSummary forecast = dashboard.getForecast();
            assertEquals(150000.0, forecast.getProjectedIncome());
            assertEquals(100000.0, forecast.getProjectedExpense());
            assertEquals(50000.0, forecast.getProjectedSavings());
            assertEquals("BLENDED_PROJECTION", forecast.getForecastMethod());
            assertEquals(0.85, forecast.getConfidenceScore());
            assertNotNull(forecast.getInsights());
            assertEquals(2, forecast.getInsights().size());
        }

        @Test
        void shouldFallbackToSimpleForecastWhenAiUnavailable() {
            // AI service returns empty (unavailable)
            when(securityUtils.getCurrentToken()).thenReturn("test-jwt-token");
            when(aiAdvisorClient.getForecast("test-jwt-token")).thenReturn(Optional.empty());

            lenient().when(transactionRepository.sumCreditsByProfileAndDateRange(any(), any(), any()))
                    .thenReturn(5000000L);
            lenient().when(transactionRepository.sumDebitsByProfileAndDateRange(any(), any(), any()))
                    .thenReturn(3000000L);

            DashboardResponse dashboard = dashboardService.getDashboard();

            ForecastSummary forecast = dashboard.getForecast();
            assertEquals("SIMPLE_PROJECTION", forecast.getForecastMethod());
            assertEquals(0.5, forecast.getConfidenceScore());
            assertNotNull(forecast.getInsights());
            assertTrue(forecast.getInsights().get(0).contains("AI Advisor"));
        }

        @Test
        void shouldFallbackToSimpleForecastWhenNoToken() {
            // No token available
            when(securityUtils.getCurrentToken()).thenReturn(null);

            lenient().when(transactionRepository.sumCreditsByProfileAndDateRange(any(), any(), any()))
                    .thenReturn(5000000L);
            lenient().when(transactionRepository.sumDebitsByProfileAndDateRange(any(), any(), any()))
                    .thenReturn(3000000L);

            DashboardResponse dashboard = dashboardService.getDashboard();

            ForecastSummary forecast = dashboard.getForecast();
            assertEquals("SIMPLE_PROJECTION", forecast.getForecastMethod());
            // AI client should not be called when no token
            verify(aiAdvisorClient, never()).getForecast(any());
        }
    }

    @Nested
    @DisplayName("Alerts Tests")
    class AlertsTests {

        @BeforeEach
        void setUpDefaultMocks() {
            setupDefaultBankMocks();
        }

        @Test
        void shouldGenerateAlertForGoalsBehindSchedule() {
            GoalsSummaryResponse goalsSummary = new GoalsSummaryResponse();
            goalsSummary.setGoalsBehindSchedule(2);
            goalsSummary.setActiveGoals(3);
            goalsSummary.setGoalsOnTrack(1);
            when(goalService.getGoalsSummary()).thenReturn(goalsSummary);
            when(savingsGoalRepository.sumCurrentAmountByProfileId(testProfileId)).thenReturn(0L);
            when(transactionRepository.findTop5ByProfileIdOrderByTxnDateDescCreatedAtDesc(testProfileId))
                    .thenReturn(Collections.emptyList());

            DashboardResponse dashboard = dashboardService.getDashboard();

            assertFalse(dashboard.getAlerts().isEmpty());
            AlertItem alert = dashboard.getAlerts().get(0);
            assertEquals("GOAL_REMINDER", alert.getType());
            assertEquals("MEDIUM", alert.getPriority());
            assertTrue(alert.getMessage().contains("2 goal(s)"));
        }

        @Test
        void shouldReturnEmptyAlertsWhenAllGoalsOnTrack() {
            GoalsSummaryResponse goalsSummary = new GoalsSummaryResponse();
            goalsSummary.setGoalsBehindSchedule(0);
            goalsSummary.setActiveGoals(3);
            goalsSummary.setGoalsOnTrack(3);
            when(goalService.getGoalsSummary()).thenReturn(goalsSummary);
            when(savingsGoalRepository.sumCurrentAmountByProfileId(testProfileId)).thenReturn(0L);
            when(transactionRepository.findTop5ByProfileIdOrderByTxnDateDescCreatedAtDesc(testProfileId))
                    .thenReturn(Collections.emptyList());

            DashboardResponse dashboard = dashboardService.getDashboard();

            assertTrue(dashboard.getAlerts().isEmpty());
        }
    }

    @Nested
    @DisplayName("Gamification Summary Tests")
    class GamificationSummaryTests {

        @BeforeEach
        void setUpDefaultMocks() {
            setupDefaultBankMocks();
            setupDefaultGoalMocks();
        }

        @Test
        void shouldReturnPlaceholderGamificationData() {
            DashboardResponse dashboard = dashboardService.getDashboard();

            GamificationSummary gamification = dashboard.getGamification();
            assertNotNull(gamification);
            assertEquals(0, gamification.getCurrentStreak());
            assertEquals(0, gamification.getLongestStreak());
            assertEquals(1, gamification.getLevel());
            assertEquals(0, gamification.getXp());
            assertTrue(gamification.getRecentBadges().isEmpty());
            assertTrue(gamification.getActiveChallenges().isEmpty());
        }
    }

    // Helper methods
    private void setupDefaultBankMocks() {
        when(bankAccountRepository.sumBalanceByProfileId(testProfileId)).thenReturn(0L);
        when(bankAccountRepository.countActiveAccountsByProfileId(testProfileId)).thenReturn(0);
        when(bankAccountRepository.getLastSyncTimeByProfileId(testProfileId)).thenReturn(null);
        lenient().when(transactionRepository.sumCreditsByProfileAndDateRange(any(), any(), any())).thenReturn(0L);
        lenient().when(transactionRepository.sumDebitsByProfileAndDateRange(any(), any(), any())).thenReturn(0L);
    }

    private void setupDefaultGoalMocks() {
        when(savingsGoalRepository.sumCurrentAmountByProfileId(testProfileId)).thenReturn(0L);
        when(transactionRepository.findTop5ByProfileIdOrderByTxnDateDescCreatedAtDesc(testProfileId))
                .thenReturn(Collections.emptyList());

        GoalsSummaryResponse goalsSummary = new GoalsSummaryResponse();
        goalsSummary.setActiveGoals(0);
        goalsSummary.setGoalsOnTrack(0);
        goalsSummary.setGoalsBehindSchedule(0);
        when(goalService.getGoalsSummary()).thenReturn(goalsSummary);
    }

    private TransactionEntity createTransaction(String description, Long amount, String direction, String category) {
        TransactionEntity txn = new TransactionEntity();
        txn.setId(UUID.randomUUID());
        txn.setProfileId(testProfileId);
        txn.setDescription(description);
        txn.setAmount(amount);
        txn.setDirection(direction);
        txn.setCategory(category);
        txn.setTxnDate(LocalDate.now());
        txn.setCurrency("INR");
        txn.setCreatedAt(Instant.now());
        return txn;
    }
}

