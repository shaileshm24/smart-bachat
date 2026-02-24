package com.ametsa.smartbachat.dto.bills;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkBillPaidRequest {

    private LocalDate paidDate;  // defaults to today

    @Positive(message = "Amount must be positive")
    private Double paidAmount;  // in rupees, defaults to bill amount
}

