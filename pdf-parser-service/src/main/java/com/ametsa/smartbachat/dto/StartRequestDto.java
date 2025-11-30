package com.ametsa.smartbachat.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class StartRequestDto {
    @NotBlank
    private String uploadId;
    @NotBlank
    private String objectName;
    @NotNull
    private UUID profileId;
    @NotBlank
    private String filename;

    // getters / setters
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
}
