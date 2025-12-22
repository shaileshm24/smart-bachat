package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.BankAccountDto;
import com.ametsa.smartbachat.dto.BankConnectionRequestDto;
import com.ametsa.smartbachat.dto.BankConnectionResponseDto;
import com.ametsa.smartbachat.service.BankConnectionService;
import org.springframework.http.ResponseEntity;
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
     * Initiate bank account connection.
     * Returns a redirect URL for user to approve consent.
     */
    @PostMapping("/connect")
    public ResponseEntity<BankConnectionResponseDto> initiateConnection(
            @Validated @RequestBody BankConnectionRequestDto request) {
        try {
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
     * Get all connected bank accounts for a profile.
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccountDto>> getAccounts(@RequestParam UUID profileId) {
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
            @PathVariable UUID accountId,
            @RequestParam UUID profileId) {
        BankAccountDto account = bankConnectionService.setPrimaryAccount(accountId, profileId);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }
}

