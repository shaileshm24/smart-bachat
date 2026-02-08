package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.goals.*;
import com.ametsa.smartbachat.service.GoalService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing savings goals.
 */
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private static final Logger log = LoggerFactory.getLogger(GoalController.class);

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    /**
     * Create a new savings goal.
     */
    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        log.info("Creating new goal: {}", request.getName());
        GoalResponse response = goalService.createGoal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all goals for the current user.
     * Optional filter by status (ACTIVE, COMPLETED, PAUSED, CANCELLED).
     */
    @GetMapping
    public ResponseEntity<List<GoalResponse>> getAllGoals(
            @RequestParam(required = false) String status) {
        List<GoalResponse> goals = goalService.getAllGoals(status);
        return ResponseEntity.ok(goals);
    }

    /**
     * Get goals summary for dashboard.
     */
    @GetMapping("/summary")
    public ResponseEntity<GoalsSummaryResponse> getGoalsSummary() {
        GoalsSummaryResponse summary = goalService.getGoalsSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Get a specific goal by ID.
     */
    @GetMapping("/{goalId}")
    public ResponseEntity<GoalResponse> getGoal(@PathVariable UUID goalId) {
        GoalResponse goal = goalService.getGoal(goalId);
        return ResponseEntity.ok(goal);
    }

    /**
     * Update an existing goal.
     */
    @PutMapping("/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateGoalRequest request) {
        log.info("Updating goal: {}", goalId);
        GoalResponse response = goalService.updateGoal(goalId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a goal.
     */
    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(@PathVariable UUID goalId) {
        log.info("Deleting goal: {}", goalId);
        goalService.deleteGoal(goalId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Add a contribution to a goal.
     */
    @PostMapping("/{goalId}/contributions")
    public ResponseEntity<ContributionResponse> addContribution(
            @PathVariable UUID goalId,
            @Valid @RequestBody ContributionRequest request) {
        log.info("Adding contribution to goal: {}", goalId);
        ContributionResponse response = goalService.addContribution(goalId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all contributions for a goal.
     */
    @GetMapping("/{goalId}/contributions")
    public ResponseEntity<List<ContributionResponse>> getContributions(@PathVariable UUID goalId) {
        List<ContributionResponse> contributions = goalService.getContributions(goalId);
        return ResponseEntity.ok(contributions);
    }
}

