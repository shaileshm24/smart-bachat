package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.export.ExportRequest;
import com.ametsa.smartbachat.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for data export functionality.
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Export transactions to CSV.
     */
    @PostMapping("/transactions")
    public ResponseEntity<byte[]> exportTransactions(@RequestBody(required = false) ExportRequest request) {
        log.info("Exporting transactions");
        if (request == null) {
            request = new ExportRequest();
        }

        String csv = exportService.exportTransactionsCsv(request);
        String filename = "transactions_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes());
    }

    /**
     * Export transactions with query parameters (GET method for easy download).
     */
    @GetMapping("/transactions")
    public ResponseEntity<byte[]> exportTransactionsGet(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String direction) {

        ExportRequest request = ExportRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .direction(direction)
                .build();

        return exportTransactions(request);
    }

    /**
     * Export savings goals to CSV.
     */
    @GetMapping("/goals")
    public ResponseEntity<byte[]> exportGoals() {
        log.info("Exporting goals");

        String csv = exportService.exportGoalsCsv();
        String filename = "goals_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes());
    }

    /**
     * Export bill reminders to CSV.
     */
    @GetMapping("/bills")
    public ResponseEntity<byte[]> exportBills() {
        log.info("Exporting bills");

        String csv = exportService.exportBillsCsv();
        String filename = "bills_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes());
    }
}

