package com.ametsa.smartbachat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartRequestDto {

    @NotBlank
    private String uploadId;

    @NotBlank
    private String objectName;

    @NotNull
    private UUID profileId;

    @NotBlank
    private String filename;
}
