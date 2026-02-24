package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.reports.MonthlyReportResponse;
import com.ametsa.smartbachat.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for financial reports.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Get monthly summary report for a specific month.
     * @param month Month in YYYY-MM format (e.g., "2026-02")
     */
    @GetMapping("/monthly/{month}")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(@PathVariable String month) {
        log.info("Generating monthly report for: {}", month);
        MonthlyReportResponse report = reportService.getMonthlyReport(month);
        return ResponseEntity.ok(report);
    }

    /**
     * Get current month's summary report.
     */
    @GetMapping("/monthly/current")
    public ResponseEntity<MonthlyReportResponse> getCurrentMonthReport() {
        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("Generating current month report: {}", currentMonth);
        MonthlyReportResponse report = reportService.getMonthlyReport(currentMonth);
        return ResponseEntity.ok(report);
    }
}

