package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.budget.*;
import com.ametsa.smartbachat.service.BudgetService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing budgets.
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private static final Logger log = LoggerFactory.getLogger(BudgetController.class);

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Get all available budget templates.
     */
    @GetMapping("/templates")
    public ResponseEntity<List<BudgetTemplateDto>> getTemplates() {
        List<BudgetTemplateDto> templates = budgetService.getTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Create a new budget for a month.
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        log.info("Creating budget for month: {}", request.getBudgetMonth());
        BudgetResponse response = budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get current month's budget.
     */
    @GetMapping("/current")
    public ResponseEntity<BudgetResponse> getCurrentBudget() {
        BudgetResponse response = budgetService.getCurrentBudget();
        return ResponseEntity.ok(response);
    }

    /**
     * Get budget for a specific month.
     */
    @GetMapping("/month/{budgetMonth}")
    public ResponseEntity<BudgetResponse> getBudgetByMonth(@PathVariable String budgetMonth) {
        BudgetResponse response = budgetService.getBudget(budgetMonth);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all budgets for the user.
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAllBudgets() {
        List<BudgetResponse> budgets = budgetService.getAllBudgets();
        return ResponseEntity.ok(budgets);
    }

    /**
     * Update an existing budget.
     */
    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable UUID budgetId,
            @Valid @RequestBody UpdateBudgetRequest request) {
        log.info("Updating budget: {}", budgetId);
        BudgetResponse response = budgetService.updateBudget(budgetId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a budget.
     */
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID budgetId) {
        log.info("Deleting budget: {}", budgetId);
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.noContent().build();
    }
}

