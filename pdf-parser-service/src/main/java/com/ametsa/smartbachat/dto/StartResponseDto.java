package com.ametsa.smartbachat.dto;

import java.util.UUID;

public class StartResponseDto {
    private UUID jobId;
    public StartResponseDto() {}
    public StartResponseDto(UUID jobId) { this.jobId = jobId; }
    public UUID getJobId() { return jobId; }
}
