package com.ametsa.smartbachat.dto.bills;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillsSummaryResponse {

    private Integer totalBills;
    private Integer activeBills;
    private Integer overdueCount;
    private Integer dueSoonCount;  // due within 7 days

    private Double totalMonthlyAmount;  // in rupees
    private Double totalOverdueAmount;  // in rupees
    private Double totalDueSoonAmount;  // in rupees

    private List<BillReminderResponse> overdueBills;
    private List<BillReminderResponse> dueSoonBills;
    private List<BillReminderResponse> upcomingBills;

    // Free tier limit info
    private Integer maxBillsAllowed;
    private Integer remainingSlots;
}

