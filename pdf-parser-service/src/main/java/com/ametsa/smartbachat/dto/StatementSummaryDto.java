package com.ametsa.smartbachat.dto;

import java.util.UUID;

/**
 * Simple summary of a bank statement: how many debit and credit rows.
 */
public class StatementSummaryDto {

    private UUID statementId;
    private int creditCount;
    private int debitCount;
    private int totalCount;

    public UUID getStatementId() {
        return statementId;
    }

    public void setStatementId(UUID statementId) {
        this.statementId = statementId;
    }

    public int getCreditCount() {
        return creditCount;
    }

    public void setCreditCount(int creditCount) {
        this.creditCount = creditCount;
    }

    public int getDebitCount() {
        return debitCount;
    }

    public void setDebitCount(int debitCount) {
        this.debitCount = debitCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}

