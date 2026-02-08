package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.goals.*;
import com.ametsa.smartbachat.entity.GoalContribution;
import com.ametsa.smartbachat.entity.SavingsGoal;
import com.ametsa.smartbachat.entity.SavingsRecommendation;
import com.ametsa.smartbachat.repository.GoalContributionRepository;
import com.ametsa.smartbachat.repository.SavingsGoalRepository;
import com.ametsa.smartbachat.repository.SavingsRecommendationRepository;
import com.ametsa.smartbachat.repository.TransactionRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalService Tests")
class GoalServiceTest {

    @Mock private SavingsGoalRepository goalRepository;
    @Mock private GoalContributionRepository contributionRepository;
    @Mock private SavingsRecommendationRepository recommendationRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SecurityUtils securityUtils;

    private GoalService goalService;
    private UUID testProfileId;

    @BeforeEach
    void setUp() {
        goalService = new GoalService(
                goalRepository, contributionRepository, recommendationRepository,
                transactionRepository, securityUtils);
        testProfileId = UUID.randomUUID();
        lenient().when(securityUtils.requireCurrentProfileId()).thenReturn(testProfileId);
    }

    @Nested
    @DisplayName("Create Goal Tests")
    class CreateGoalTests {

        @Test
        void shouldCreateGoalSuccessfully() {
            CreateGoalRequest request = new CreateGoalRequest();
            request.setName("Goa Trip");
            request.setGoalType("TRAVEL");
            request.setTargetAmount(50000.0);
            request.setDeadline(LocalDate.now().plusMonths(6));
            request.setPriority("HIGH");

            when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> {
                SavingsGoal goal = inv.getArgument(0);
                goal.setId(UUID.randomUUID());
                goal.setCreatedAt(Instant.now());
                return goal;
            });

            GoalResponse response = goalService.createGoal(request);

            assertNotNull(response);
            assertEquals("Goa Trip", response.getName());
            assertEquals("TRAVEL", response.getGoalType());
            assertEquals(50000.0, response.getTargetAmount());
            assertEquals("HIGH", response.getPriority());
            assertEquals("ACTIVE", response.getStatus());
            assertNotNull(response.getSuggestedMonthlySaving());
            verify(goalRepository, times(1)).save(any(SavingsGoal.class));
        }

        @Test
        void shouldSetDefaultPriorityWhenNotProvided() {
            CreateGoalRequest request = new CreateGoalRequest();
            request.setName("Emergency Fund");
            request.setGoalType("EMERGENCY");
            request.setTargetAmount(100000.0);

            when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> {
                SavingsGoal goal = inv.getArgument(0);
                goal.setId(UUID.randomUUID());
                goal.setCreatedAt(Instant.now());
                return goal;
            });

            GoalResponse response = goalService.createGoal(request);

            assertEquals("MEDIUM", response.getPriority());
        }

        @Test
        void shouldCalculateSuggestedMonthlySaving() {
            CreateGoalRequest request = new CreateGoalRequest();
            request.setName("Car Down Payment");
            request.setGoalType("VEHICLE");
            request.setTargetAmount(120000.0);
            request.setDeadline(LocalDate.now().plusMonths(12));

            when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> {
                SavingsGoal goal = inv.getArgument(0);
                goal.setId(UUID.randomUUID());
                goal.setCreatedAt(Instant.now());
                return goal;
            });

            GoalResponse response = goalService.createGoal(request);

            // 120000 / 12 months = 10000 per month
            assertEquals(10000.0, response.getSuggestedMonthlySaving(), 0.01);
        }
    }

    @Nested
    @DisplayName("Get Goals Tests")
    class GetGoalsTests {

        @Test
        void shouldReturnAllGoalsForUser() {
            List<SavingsGoal> goals = Arrays.asList(
                    createGoal("Goal 1", 50000L, 10000L),
                    createGoal("Goal 2", 100000L, 25000L)
            );
            when(goalRepository.findByProfileIdOrderByCreatedAtDesc(testProfileId)).thenReturn(goals);

            List<GoalResponse> responses = goalService.getAllGoals(null);

            assertEquals(2, responses.size());
            assertEquals("Goal 1", responses.get(0).getName());
            assertEquals("Goal 2", responses.get(1).getName());
        }

        @Test
        void shouldFilterGoalsByStatus() {
            SavingsGoal activeGoal = createGoal("Active Goal", 50000L, 10000L);
            when(goalRepository.findByProfileIdAndStatusOrderByCreatedAtDesc(testProfileId, "ACTIVE"))
                    .thenReturn(List.of(activeGoal));

            List<GoalResponse> responses = goalService.getAllGoals("ACTIVE");

            assertEquals(1, responses.size());
            assertEquals("Active Goal", responses.get(0).getName());
        }
    }

    @Nested
    @DisplayName("Get Single Goal Tests")
    class GetSingleGoalTests {

        @Test
        void shouldReturnGoalWithContributions() {
            UUID goalId = UUID.randomUUID();
            SavingsGoal goal = createGoal("Test Goal", 50000L, 15000L);
            goal.setId(goalId);

            GoalContribution contribution = new GoalContribution();
            contribution.setId(UUID.randomUUID());
            contribution.setGoalId(goalId);
            contribution.setAmount(5000L);
            contribution.setContributionDate(LocalDate.now());
            contribution.setSource("MANUAL");
            contribution.setCreatedAt(Instant.now());

            when(goalRepository.findByIdAndProfileId(goalId, testProfileId)).thenReturn(Optional.of(goal));
            when(contributionRepository.findTop5ByGoalIdOrderByContributionDateDesc(goalId))
                    .thenReturn(List.of(contribution));

            GoalResponse response = goalService.getGoal(goalId);

            assertNotNull(response);
            assertEquals("Test Goal", response.getName());
            assertEquals(1, response.getRecentContributions().size());
            assertEquals(50.0, response.getRecentContributions().get(0).getAmount());
        }

        @Test
        void shouldThrowExceptionWhenGoalNotFound() {
            UUID goalId = UUID.randomUUID();
            when(goalRepository.findByIdAndProfileId(goalId, testProfileId)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> goalService.getGoal(goalId));
        }
    }

    @Nested
    @DisplayName("Update Goal Tests")
    class UpdateGoalTests {

        @Test
        void shouldUpdateGoalFields() {
            UUID goalId = UUID.randomUUID();
            SavingsGoal existingGoal = createGoal("Old Name", 50000L, 10000L);
            existingGoal.setId(goalId);

            UpdateGoalRequest request = new UpdateGoalRequest();
            request.setName("New Name");
            request.setTargetAmount(75000.0);
            request.setPriority("HIGH");

            when(goalRepository.findByIdAndProfileId(goalId, testProfileId)).thenReturn(Optional.of(existingGoal));
            when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));

            GoalResponse response = goalService.updateGoal(goalId, request);

            assertEquals("New Name", response.getName());
            assertEquals(75000.0, response.getTargetAmount());
            assertEquals("HIGH", response.getPriority());
        }

        @Test
        void shouldOnlyUpdateProvidedFields() {
            UUID goalId = UUID.randomUUID();
            SavingsGoal existingGoal = createGoal("Original Name", 50000L, 10000L);
            existingGoal.setId(goalId);
            existingGoal.setPriority("LOW");

            UpdateGoalRequest request = new UpdateGoalRequest();
            request.setTargetAmount(60000.0); // Only update target amount

            when(goalRepository.findByIdAndProfileId(goalId, testProfileId)).thenReturn(Optional.of(existingGoal));
            when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));

            GoalResponse response = goalService.updateGoal(goalId, request);

            assertEquals("Original Name", response.getName()); // Unchanged
            assertEquals("LOW", response.getPriority()); // Unchanged
            assertEquals(60000.0, response.getTargetAmount()); // Updated
        }
    }

    @Nested
    @DisplayName("Delete Goal Tests")
    class DeleteGoalTests {

        @Test
        void shouldDeleteGoalAndContributions() {
            UUID goalId = UUID.randomUUID();
            SavingsGoal goal = createGoal("To Delete", 50000L, 10000L);
            goal.setId(goalId);

            when(goalRepository.findByIdAndProfileId(goalId, testProfileId)).thenReturn(Optional.of(goal));

            goalService.deleteGoal(goalId);

            verify(contributionRepository, times(1)).deleteByGoalId(goalId);
            verify(recommendationRepository, times(1)).dismissAllForGoal(goalId);
            verify(goalRepository, times(1)).delete(goal);
        }
    }

    @Nested
    @DisplayName("Contribution Tests")
    class ContributionTests {

        @Test
        void shouldAddContributionAndUpdateGoal() {
            UUID goalId = UUID.randomUUID();
            SavingsGoal goal = createGoal("Test Goal", 5000000L, 1000000L); // 50000, 10000 in rupees
            goal.setId(goalId);

            ContributionRequest request = new ContributionRequest();
            request.setAmount(5000.0);
            request.setNotes("Monthly saving");

            when(goalRepository.findByIdAndProfileId(goalId, testProfileId)).thenReturn(Optional.of(goal));
            when(contributionRepository.save(any(GoalContribution.class))).thenAnswer(inv -> {
                GoalContribution c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                c.setCreatedAt(Instant.now());
                return c;
            });
            when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));

            ContributionResponse response = goalService.addContribution(goalId, request);

            assertNotNull(response);
            assertEquals(5000.0, response.getAmount());
            assertEquals("MANUAL", response.getSource());
            verify(goalRepository, times(1)).save(any(SavingsGoal.class));
        }

        @Test
        void shouldMarkGoalAsCompletedWhenTargetReached() {
            UUID goalId = UUID.randomUUID();
            SavingsGoal goal = createGoal("Almost Done", 5000000L, 4500000L); // 50000, 45000 in rupees
            goal.setId(goalId);

            ContributionRequest request = new ContributionRequest();
            request.setAmount(5000.0); // This will complete the goal

            when(goalRepository.findByIdAndProfileId(goalId, testProfileId)).thenReturn(Optional.of(goal));
            when(contributionRepository.save(any(GoalContribution.class))).thenAnswer(inv -> {
                GoalContribution c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                c.setCreatedAt(Instant.now());
                return c;
            });
            when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));

            goalService.addContribution(goalId, request);

            verify(goalRepository).save(argThat(g -> "COMPLETED".equals(g.getStatus())));
        }

        @Test
        void shouldGetContributionsForGoal() {
            UUID goalId = UUID.randomUUID();
            SavingsGoal goal = createGoal("Test Goal", 50000L, 15000L);
            goal.setId(goalId);

            List<GoalContribution> contributions = Arrays.asList(
                    createContribution(goalId, 5000L, LocalDate.now()),
                    createContribution(goalId, 10000L, LocalDate.now().minusDays(7))
            );

            when(goalRepository.findByIdAndProfileId(goalId, testProfileId)).thenReturn(Optional.of(goal));
            when(contributionRepository.findByGoalIdOrderByContributionDateDesc(goalId)).thenReturn(contributions);

            List<ContributionResponse> responses = goalService.getContributions(goalId);

            assertEquals(2, responses.size());
        }
    }

    @Nested
    @DisplayName("Goals Summary Tests")
    class GoalsSummaryTests {

        @Test
        void shouldReturnCorrectSummary() {
            SavingsGoal activeGoal = createGoal("Active", 5000000L, 2500000L);
            activeGoal.setStatus("ACTIVE");
            SavingsGoal completedGoal = createGoal("Completed", 3000000L, 3000000L);
            completedGoal.setStatus("COMPLETED");

            when(goalRepository.findByProfileIdOrderByCreatedAtDesc(testProfileId))
                    .thenReturn(Arrays.asList(activeGoal, completedGoal));
            when(goalRepository.sumTargetAmountByProfileId(testProfileId)).thenReturn(8000000L);
            when(goalRepository.sumCurrentAmountByProfileId(testProfileId)).thenReturn(5500000L);
            when(recommendationRepository.findActiveRecommendations(eq(testProfileId), any()))
                    .thenReturn(Collections.emptyList());

            GoalsSummaryResponse summary = goalService.getGoalsSummary();

            assertEquals(2, summary.getTotalGoals());
            assertEquals(1, summary.getActiveGoals());
            assertEquals(1, summary.getCompletedGoals());
            assertEquals(80000.0, summary.getTotalTargetAmount());
            assertEquals(55000.0, summary.getTotalSavedAmount());
        }
    }

    @Nested
    @DisplayName("Progress Calculation Tests")
    class ProgressCalculationTests {

        @Test
        void shouldCalculateProgressPercent() {
            SavingsGoal goal = createGoal("Test", 10000000L, 2500000L); // 100000, 25000 in rupees
            when(goalRepository.findByProfileIdOrderByCreatedAtDesc(testProfileId)).thenReturn(List.of(goal));

            List<GoalResponse> responses = goalService.getAllGoals(null);

            assertEquals(25.0, responses.get(0).getProgressPercent(), 0.01);
        }

        @Test
        void shouldCalculateRemainingAmount() {
            SavingsGoal goal = createGoal("Test", 10000000L, 2500000L);
            when(goalRepository.findByProfileIdOrderByCreatedAtDesc(testProfileId)).thenReturn(List.of(goal));

            List<GoalResponse> responses = goalService.getAllGoals(null);

            assertEquals(75000.0, responses.get(0).getRemainingAmount(), 0.01);
        }
    }

    private SavingsGoal createGoal(String name, Long targetAmount, Long currentAmount) {
        SavingsGoal goal = new SavingsGoal();
        goal.setId(UUID.randomUUID());
        goal.setProfileId(testProfileId);
        goal.setName(name);
        goal.setGoalType("GENERAL");
        goal.setTargetAmount(targetAmount);
        goal.setCurrentAmount(currentAmount);
        goal.setPriority("MEDIUM");
        goal.setCreatedAt(Instant.now());
        return goal;
    }

    private GoalContribution createContribution(UUID goalId, Long amount, LocalDate date) {
        GoalContribution contribution = new GoalContribution();
        contribution.setId(UUID.randomUUID());
        contribution.setGoalId(goalId);
        contribution.setAmount(amount);
        contribution.setContributionDate(date);
        contribution.setSource("MANUAL");
        contribution.setCreatedAt(Instant.now());
        return contribution;
    }
}

