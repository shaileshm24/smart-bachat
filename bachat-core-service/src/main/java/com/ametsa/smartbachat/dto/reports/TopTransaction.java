package com.ametsa.smartbachat.dto.reports;

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
public class TopTransaction {

    private UUID id;
    private LocalDate date;
    private Double amount;  // in rupees
    private String description;
    private String category;
    private String merchant;
}

