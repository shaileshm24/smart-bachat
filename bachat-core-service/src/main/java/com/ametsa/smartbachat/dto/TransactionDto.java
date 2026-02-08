package com.ametsa.smartbachat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private UUID id;
    private UUID statementId;
    private UUID profileId;
    private LocalDate txnDate;

    /**
     * Amount and balance are exposed in rupees (e.g. 199.99),
     * even though we store minor units (paisa) internally.
     */
    private Double amount;

    /**
     * Explicit indicator so clients don't have to infer from sign.
     * Values: "DEBIT" or "CREDIT" (null/absent for zero amount)
     */
    private String direction;
    private String currency;
    private String txnType;
    private String description;
    private String merchant;
    private Double balance;
    private String category;
    private String subCategory;
}
