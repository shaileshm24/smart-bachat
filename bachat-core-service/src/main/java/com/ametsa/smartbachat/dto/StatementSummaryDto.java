package com.ametsa.smartbachat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Simple summary of a bank statement: how many debit and credit rows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementSummaryDto {

    private UUID statementId;
    private int creditCount;
    private int debitCount;
    private int totalCount;
}

