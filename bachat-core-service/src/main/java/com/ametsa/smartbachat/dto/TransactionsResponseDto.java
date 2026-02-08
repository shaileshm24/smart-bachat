package com.ametsa.smartbachat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for transactions list + statement summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionsResponseDto {

    private StatementSummaryDto summary;
    private List<TransactionDto> transactions;
}

