package com.ametsa.smartbachat.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Balance summary across all connected bank accounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSummary {

    /**
     * Total balance across all active accounts (in rupees).
     */
    private Double totalBalance;

    /**
     * Number of active connected accounts.
     */
    private Integer accountCount;

    /**
     * Last sync timestamp across all accounts.
     */
    private Instant lastSyncedAt;

    /**
     * Currency code (default: INR).
     */
    private String currency;
}

