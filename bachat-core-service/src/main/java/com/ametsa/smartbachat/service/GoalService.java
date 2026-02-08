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

/**
 * Service for managing savings goals and contributions.
 */
@Service
public class GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalService.class);
    private static final long PAISA_MULTIPLIER = 100L;

    private final SavingsGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final SavingsRecommendationRepository recommendationRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;

    public GoalService(
            SavingsGoalRepository goalRepository,
            GoalContributionRepository contributionRepository,
            SavingsRecommendationRepository recommendationRepository,
            TransactionRepository transactionRepository,
            SecurityUtils securityUtils) {
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.recommendationRepository = recommendationRepository;
        this.transactionRepository = transactionRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Create a new savings goal.
     */
    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        log.info("Creating goal '{}' for profile: {}", request.getName(), profileId);

        SavingsGoal goal = new SavingsGoal();
        goal.setProfileId(profileId);
        goal.setName(request.getName());
        goal.setGoalType(request.getGoalType());
        goal.setTargetAmount(rupeesToPaisa(request.getTargetAmount()));
        goal.setDeadline(request.getDeadline());
        goal.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        goal.setIcon(request.getIcon());
        goal.setColor(request.getColor());
        goal.setNotes(request.getNotes());

        // Calculate suggested monthly saving if deadline is set
        if (request.getDeadline() != null) {
            long monthsRemaining = ChronoUnit.MONTHS.between(LocalDate.now(), request.getDeadline());
            if (monthsRemaining > 0) {
                long suggestedMonthly = goal.getTargetAmount() / monthsRemaining;
                goal.setSuggestedMonthlySaving(suggestedMonthly);
            }
        }

        goal = goalRepository.save(goal);
        log.info("Created goal with ID: {}", goal.getId());

        return mapToGoalResponse(goal);
    }

    /**
     * Get all goals for the current user.
     */
    public List<GoalResponse> getAllGoals(String status) {
        UUID profileId = securityUtils.requireCurrentProfileId();

        List<SavingsGoal> goals;
        if (status != null && !status.isEmpty()) {
            goals = goalRepository.findByProfileIdAndStatusOrderByCreatedAtDesc(profileId, status);
        } else {
            goals = goalRepository.findByProfileIdOrderByCreatedAtDesc(profileId);
        }

        return goals.stream()
                .map(this::mapToGoalResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific goal by ID.
     */
    public GoalResponse getGoal(UUID goalId) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        SavingsGoal goal = goalRepository.findByIdAndProfileId(goalId, profileId)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + goalId));

        GoalResponse response = mapToGoalResponse(goal);

        // Add recent contributions
        List<GoalContribution> contributions = contributionRepository
                .findTop5ByGoalIdOrderByContributionDateDesc(goalId);
        response.setRecentContributions(contributions.stream()
                .map(this::mapToContributionResponse)
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * Update an existing goal.
     */
    @Transactional
    public GoalResponse updateGoal(UUID goalId, UpdateGoalRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        SavingsGoal goal = goalRepository.findByIdAndProfileId(goalId, profileId)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + goalId));

        if (request.getName() != null) goal.setName(request.getName());
        if (request.getGoalType() != null) goal.setGoalType(request.getGoalType());
        if (request.getTargetAmount() != null) goal.setTargetAmount(rupeesToPaisa(request.getTargetAmount()));
        if (request.getDeadline() != null) goal.setDeadline(request.getDeadline());
        if (request.getPriority() != null) goal.setPriority(request.getPriority());
        if (request.getStatus() != null) goal.setStatus(request.getStatus());
        if (request.getIcon() != null) goal.setIcon(request.getIcon());
        if (request.getColor() != null) goal.setColor(request.getColor());
        if (request.getNotes() != null) goal.setNotes(request.getNotes());

        goal.setUpdatedAt(Instant.now());
        recalculateSuggestedMonthlySaving(goal);

        goal = goalRepository.save(goal);
        log.info("Updated goal: {}", goalId);

        return mapToGoalResponse(goal);
    }

    // Helper methods - will be added via str-replace-editor
    private Long rupeesToPaisa(Double rupees) {
        return rupees != null ? Math.round(rupees * PAISA_MULTIPLIER) : 0L;
    }

    private Double paisaToRupees(Long paisa) {
        return paisa != null ? paisa / (double) PAISA_MULTIPLIER : 0.0;
    }

    private void recalculateSuggestedMonthlySaving(SavingsGoal goal) {
        if (goal.getDeadline() != null && goal.getRemainingAmount() > 0) {
            long monthsRemaining = ChronoUnit.MONTHS.between(LocalDate.now(), goal.getDeadline());
            if (monthsRemaining > 0) {
                goal.setSuggestedMonthlySaving(goal.getRemainingAmount() / monthsRemaining);
            }
        }
    }

    /**
     * Delete a goal.
     */
    @Transactional
    public void deleteGoal(UUID goalId) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        SavingsGoal goal = goalRepository.findByIdAndProfileId(goalId, profileId)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + goalId));

        // Delete contributions first
        contributionRepository.deleteByGoalId(goalId);
        // Dismiss recommendations
        recommendationRepository.dismissAllForGoal(goalId);
        // Delete the goal
        goalRepository.delete(goal);
        log.info("Deleted goal: {}", goalId);
    }

    /**
     * Add a contribution to a goal.
     */
    @Transactional
    public ContributionResponse addContribution(UUID goalId, ContributionRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        SavingsGoal goal = goalRepository.findByIdAndProfileId(goalId, profileId)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + goalId));

        GoalContribution contribution = new GoalContribution();
        contribution.setGoalId(goalId);
        contribution.setAmount(rupeesToPaisa(request.getAmount()));
        contribution.setContributionDate(request.getContributionDate() != null
                ? request.getContributionDate() : LocalDate.now());
        contribution.setSource(request.getSource() != null ? request.getSource() : "MANUAL");
        contribution.setTransactionId(request.getTransactionId());
        contribution.setNotes(request.getNotes());

        contribution = contributionRepository.save(contribution);

        // Update goal's current amount
        goal.setCurrentAmount(goal.getCurrentAmount() + contribution.getAmount());
        goal.setUpdatedAt(Instant.now());

        // Check if goal is completed
        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setStatus("COMPLETED");
            log.info("Goal {} completed!", goalId);
        }

        recalculateSuggestedMonthlySaving(goal);
        goalRepository.save(goal);

        log.info("Added contribution of {} to goal {}", request.getAmount(), goalId);
        return mapToContributionResponse(contribution);
    }

    /**
     * Get contributions for a goal.
     */
    public List<ContributionResponse> getContributions(UUID goalId) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        // Verify goal belongs to user
        goalRepository.findByIdAndProfileId(goalId, profileId)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + goalId));

        return contributionRepository.findByGoalIdOrderByContributionDateDesc(goalId)
                .stream()
                .map(this::mapToContributionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get goals summary for dashboard.
     */
    public GoalsSummaryResponse getGoalsSummary() {
        UUID profileId = securityUtils.requireCurrentProfileId();

        List<SavingsGoal> allGoals = goalRepository.findByProfileIdOrderByCreatedAtDesc(profileId);
        List<SavingsGoal> activeGoals = allGoals.stream()
                .filter(g -> "ACTIVE".equals(g.getStatus()))
                .collect(Collectors.toList());

        Long totalTarget = goalRepository.sumTargetAmountByProfileId(profileId);
        Long totalSaved = goalRepository.sumCurrentAmountByProfileId(profileId);

        GoalsSummaryResponse summary = new GoalsSummaryResponse();
        summary.setTotalGoals(allGoals.size());
        summary.setActiveGoals((int) activeGoals.size());
        summary.setCompletedGoals((int) allGoals.stream()
                .filter(g -> "COMPLETED".equals(g.getStatus())).count());
        summary.setTotalTargetAmount(paisaToRupees(totalTarget));
        summary.setTotalSavedAmount(paisaToRupees(totalSaved));
        summary.setOverallProgressPercent(totalTarget > 0 ? (totalSaved * 100.0 / totalTarget) : 0.0);

        // Calculate goals on track vs behind
        int onTrack = 0, behind = 0;
        double totalSuggested = 0;
        for (SavingsGoal goal : activeGoals) {
            if (isGoalOnTrack(goal)) onTrack++; else behind++;
            if (goal.getSuggestedMonthlySaving() != null) {
                totalSuggested += goal.getSuggestedMonthlySaving();
            }
        }
        summary.setGoalsOnTrack(onTrack);
        summary.setGoalsBehindSchedule(behind);
        summary.setTotalSuggestedMonthlySaving(paisaToRupees((long) totalSuggested));

        summary.setGoals(allGoals.stream().map(this::mapToGoalResponse).collect(Collectors.toList()));

        // Get active recommendations
        List<SavingsRecommendation> recommendations = recommendationRepository
                .findActiveRecommendations(profileId, LocalDate.now());
        summary.setRecommendations(recommendations.stream()
                .map(this::mapToRecommendationResponse)
                .collect(Collectors.toList()));

        return summary;
    }

    private boolean isGoalOnTrack(SavingsGoal goal) {
        if (goal.getDeadline() == null) return true;
        if (goal.getCurrentAmount() >= goal.getTargetAmount()) return true;

        long totalDays = ChronoUnit.DAYS.between(goal.getCreatedAt(), goal.getDeadline().atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        long daysElapsed = ChronoUnit.DAYS.between(goal.getCreatedAt(), Instant.now());
        if (totalDays <= 0) return false;

        double expectedProgress = (daysElapsed * 100.0) / totalDays;
        return goal.getProgressPercent() >= expectedProgress * 0.8; // 80% tolerance
    }

    private GoalResponse mapToGoalResponse(SavingsGoal goal) {
        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setName(goal.getName());
        response.setGoalType(goal.getGoalType());
        response.setTargetAmount(paisaToRupees(goal.getTargetAmount()));
        response.setCurrentAmount(paisaToRupees(goal.getCurrentAmount()));
        response.setRemainingAmount(paisaToRupees(goal.getRemainingAmount()));
        response.setProgressPercent(goal.getProgressPercent());
        response.setDeadline(goal.getDeadline());
        response.setPriority(goal.getPriority());
        response.setStatus(goal.getStatus());
        response.setIcon(goal.getIcon());
        response.setColor(goal.getColor());
        response.setNotes(goal.getNotes());
        response.setProjectedCompletionDate(goal.getProjectedCompletionDate());
        response.setSuggestedMonthlySaving(paisaToRupees(goal.getSuggestedMonthlySaving()));
        response.setCreatedAt(goal.getCreatedAt());
        response.setUpdatedAt(goal.getUpdatedAt());

        if (goal.getDeadline() != null) {
            response.setDaysRemaining((int) ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline()));
        }
        response.setIsOnTrack(isGoalOnTrack(goal));

        return response;
    }

    private ContributionResponse mapToContributionResponse(GoalContribution contribution) {
        ContributionResponse response = new ContributionResponse();
        response.setId(contribution.getId());
        response.setGoalId(contribution.getGoalId());
        response.setAmount(paisaToRupees(contribution.getAmount()));
        response.setContributionDate(contribution.getContributionDate());
        response.setSource(contribution.getSource());
        response.setTransactionId(contribution.getTransactionId());
        response.setNotes(contribution.getNotes());
        response.setCreatedAt(contribution.getCreatedAt());
        return response;
    }

    private RecommendationResponse mapToRecommendationResponse(SavingsRecommendation rec) {
        RecommendationResponse response = new RecommendationResponse();
        response.setId(rec.getId());
        response.setGoalId(rec.getGoalId());
        response.setRecommendationType(rec.getRecommendationType());
        response.setMessage(rec.getMessage());
        response.setSuggestedAmount(paisaToRupees(rec.getSuggestedAmount()));
        response.setConfidenceScore(rec.getConfidenceScore() != null
                ? rec.getConfidenceScore().doubleValue() : null);
        response.setValidUntil(rec.getValidUntil());
        response.setCategory(rec.getCategory());
        response.setPotentialSavings(paisaToRupees(rec.getPotentialSavings()));
        response.setCreatedAt(rec.getCreatedAt());
        return response;
    }
}

