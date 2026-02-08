package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.dashboard.DashboardResponse;
import com.ametsa.smartbachat.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the dashboard API.
 * Provides aggregated data for the mobile app home screen.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Get aggregated dashboard data for the authenticated user.
     * 
     * Returns:
     * - Total savings across all goals
     * - Motivational nudge message
     * - Balance summary across all connected accounts
     * - Savings goal progress summary
     * - Gamification data (streaks, badges, challenges)
     * - Month's financial forecast
     * - Active alerts and reminders
     * - Recent transactions (last 5)
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard() {
        log.info("Fetching dashboard data");
        DashboardResponse dashboard = dashboardService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }
}

