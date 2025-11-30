package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statement_metadata")

public class StatementMetadata {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "upload_id")
    private String uploadId;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "filename")
    private String filename;

    @Column(name = "status")
    private String status; // PENDING, PROCESSING, PASSWORD_REQUIRED, DONE, FAILED

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "bucket_name")
    private String bucketName;

    @Column(name="file_path")
    private String objectPath; // uploads/user123/file.pdf


    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    public StatementMetadata() {}

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public String getObjectPath() { return objectPath; }
    public void setObjectPath(String objectPath) { this.objectPath = objectPath; }
    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getBucketName() { return errorMessage; }
    public void setBucketName(String errorMessage) { this.errorMessage = errorMessage; }
}
