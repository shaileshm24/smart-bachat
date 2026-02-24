package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.bills.*;
import com.ametsa.smartbachat.service.BillReminderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing bill reminders.
 */
@RestController
@RequestMapping("/api/bills")
public class BillReminderController {

    private static final Logger log = LoggerFactory.getLogger(BillReminderController.class);

    private final BillReminderService billReminderService;

    public BillReminderController(BillReminderService billReminderService) {
        this.billReminderService = billReminderService;
    }

    /**
     * Create a new bill reminder.
     */
    @PostMapping
    public ResponseEntity<BillReminderResponse> createReminder(@Valid @RequestBody CreateBillReminderRequest request) {
        log.info("Creating bill reminder: {}", request.getName());
        BillReminderResponse response = billReminderService.createReminder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all bill reminders.
     */
    @GetMapping
    public ResponseEntity<List<BillReminderResponse>> getAllReminders() {
        List<BillReminderResponse> reminders = billReminderService.getAllReminders();
        return ResponseEntity.ok(reminders);
    }

    /**
     * Get active bill reminders only.
     */
    @GetMapping("/active")
    public ResponseEntity<List<BillReminderResponse>> getActiveReminders() {
        List<BillReminderResponse> reminders = billReminderService.getActiveReminders();
        return ResponseEntity.ok(reminders);
    }

    /**
     * Get bills summary (overdue, due soon, upcoming).
     */
    @GetMapping("/summary")
    public ResponseEntity<BillsSummaryResponse> getSummary() {
        BillsSummaryResponse summary = billReminderService.getSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Get a specific bill reminder.
     */
    @GetMapping("/{reminderId}")
    public ResponseEntity<BillReminderResponse> getReminder(@PathVariable UUID reminderId) {
        BillReminderResponse response = billReminderService.getReminder(reminderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a bill reminder.
     */
    @PutMapping("/{reminderId}")
    public ResponseEntity<BillReminderResponse> updateReminder(
            @PathVariable UUID reminderId,
            @Valid @RequestBody UpdateBillReminderRequest request) {
        log.info("Updating bill reminder: {}", reminderId);
        BillReminderResponse response = billReminderService.updateReminder(reminderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a bill as paid.
     */
    @PostMapping("/{reminderId}/mark-paid")
    public ResponseEntity<BillReminderResponse> markAsPaid(
            @PathVariable UUID reminderId,
            @Valid @RequestBody(required = false) MarkBillPaidRequest request) {
        log.info("Marking bill as paid: {}", reminderId);
        if (request == null) {
            request = new MarkBillPaidRequest();
        }
        BillReminderResponse response = billReminderService.markAsPaid(reminderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a bill reminder.
     */
    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable UUID reminderId) {
        log.info("Deleting bill reminder: {}", reminderId);
        billReminderService.deleteReminder(reminderId);
        return ResponseEntity.noContent().build();
    }
}

