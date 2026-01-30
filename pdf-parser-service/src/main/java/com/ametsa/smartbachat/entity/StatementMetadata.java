package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statement_metadata", indexes = {
        @Index(name = "idx_statement_user", columnList = "user_id"),
        @Index(name = "idx_statement_profile", columnList = "profile_id")
})
public class StatementMetadata {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    // User ID from UAM service (logged-in user)
    @Column(name = "user_id")
    private UUID userId;

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
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
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
	    public String getBucketName() { return bucketName; }
	    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
}
