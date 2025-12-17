package com.ametsa.smartbachat.dto;

import java.util.List;

/**
 * Response wrapper for transactions list + statement summary.
 */
public class TransactionsResponseDto {

    private StatementSummaryDto summary;
    private List<TransactionDto> transactions;

    public StatementSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(StatementSummaryDto summary) {
        this.summary = summary;
    }

    public List<TransactionDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDto> transactions) {
        this.transactions = transactions;
    }
}

