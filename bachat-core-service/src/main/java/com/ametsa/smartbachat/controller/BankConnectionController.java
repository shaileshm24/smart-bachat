package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.BankAccountDto;
import com.ametsa.smartbachat.dto.BankConnectionRequestDto;
import com.ametsa.smartbachat.dto.BankConnectionResponseDto;
import com.ametsa.smartbachat.security.UserPrincipal;
import com.ametsa.smartbachat.service.BankConnectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for bank account connection via Account Aggregator.
 */
@RestController
@RequestMapping("/api/v1/bank")
public class BankConnectionController {

    private final BankConnectionService bankConnectionService;

    public BankConnectionController(BankConnectionService bankConnectionService) {
        this.bankConnectionService = bankConnectionService;
    }

    /**
     * Get the profile ID from the authenticated user.
     */
    private UUID getProfileId(UserPrincipal principal) {
        if (principal == null || principal.getProfileId() == null) {
            throw new RuntimeException("User profile not found. Please complete your profile setup.");
        }
        return principal.getProfileId();
    }

    /**
     * Initiate bank account connection.
     * Returns a redirect URL for user to approve consent.
     */
    @PostMapping("/connect")
    public ResponseEntity<BankConnectionResponseDto> initiateConnection(
            @AuthenticationPrincipal UserPrincipal principal,
            @Validated @RequestBody BankConnectionRequestDto request) {
        try {
            // Set profileId from authenticated user if not provided
            if (request.getProfileId() == null) {
                request.setProfileId(getProfileId(principal));
            }
            BankConnectionResponseDto response = bankConnectionService.initiateConnection(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            BankConnectionResponseDto error = new BankConnectionResponseDto();
            error.setStatus("FAILED");
            error.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all connected bank accounts for the authenticated user.
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccountDto>> getAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        UUID profileId = getProfileId(principal);
        List<BankAccountDto> accounts = bankConnectionService.getAccountsForProfile(profileId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get a specific bank account by ID.
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<BankAccountDto> getAccount(@PathVariable UUID accountId) {
        BankAccountDto account = bankConnectionService.getAccount(accountId);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    /**
     * Sync transactions for a bank account.
     * Fetches latest transactions from the bank via AA.
     */
    @PostMapping("/accounts/{accountId}/sync")
    public ResponseEntity<BankConnectionResponseDto> syncAccount(@PathVariable UUID accountId) {
        try {
            BankConnectionResponseDto response = bankConnectionService.syncAccount(accountId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            BankConnectionResponseDto error = new BankConnectionResponseDto();
            error.setStatus("FAILED");
            error.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Disconnect a bank account (revoke consent).
     */
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<BankConnectionResponseDto> disconnectAccount(@PathVariable UUID accountId) {
        try {
            BankConnectionResponseDto response = bankConnectionService.disconnectAccount(accountId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            BankConnectionResponseDto error = new BankConnectionResponseDto();
            error.setStatus("FAILED");
            error.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get consent status for a bank account.
     */
    @GetMapping("/accounts/{accountId}/consent-status")
    public ResponseEntity<BankConnectionResponseDto> getConsentStatus(@PathVariable UUID accountId) {
        try {
            BankConnectionResponseDto response = bankConnectionService.getConsentStatus(accountId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            BankConnectionResponseDto error = new BankConnectionResponseDto();
            error.setStatus("FAILED");
            error.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update account nickname.
     */
    @PatchMapping("/accounts/{accountId}/nickname")
    public ResponseEntity<BankAccountDto> updateNickname(
            @PathVariable UUID accountId,
            @RequestParam String nickname) {
        BankAccountDto account = bankConnectionService.updateNickname(accountId, nickname);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    /**
     * Set account as primary.
     */
    @PostMapping("/accounts/{accountId}/set-primary")
    public ResponseEntity<BankAccountDto> setPrimary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID accountId) {
        UUID profileId = getProfileId(principal);
        BankAccountDto account = bankConnectionService.setPrimaryAccount(accountId, profileId);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    /**
     * Get transactions for a specific bank account.
     */
    @GetMapping("/accounts/{userId}/transactions")
    public ResponseEntity<?> getTransactions(
            @PathVariable UUID userId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            var transactions = bankConnectionService.getTransactionsForAccount(
                    userId, fromDate, toDate, page, size);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get sync history for a bank account.
     */
    @GetMapping("/accounts/{accountId}/sync-history")
    public ResponseEntity<?> getSyncHistory(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            var history = bankConnectionService.getSyncHistory(accountId, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }
}

