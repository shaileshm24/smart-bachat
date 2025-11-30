package com.ametsa.smartbachat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class UploadRequestDto {
    @NotNull
    private UUID profileId;
    @NotBlank
    private String filename;
    // getters & setters
    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
}
