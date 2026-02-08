package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.StartResponseDto;
import com.ametsa.smartbachat.entity.StatementMetadata;
import com.ametsa.smartbachat.repository.StatementMetadataRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JobService {

    private final StatementMetadataRepository metadataRepository;
    private final Publisher publisher;
    private final SecurityUtils securityUtils;
    private final Gson gson = new Gson();

    public JobService(StatementMetadataRepository metadataRepository, Publisher publisher, SecurityUtils securityUtils) {
        this.metadataRepository = metadataRepository;
        this.publisher = publisher;
        this.securityUtils = securityUtils;
    }

    public StartResponseDto startJob(String uploadId, String objectName, UUID profileId, String filename) throws Exception {
        UUID userId = securityUtils.requireCurrentUserId();
        UUID jobId = UUID.randomUUID();
            StatementMetadata meta = new StatementMetadata();
            meta.setId(jobId);
            meta.setUserId(userId);
            meta.setUploadId(uploadId);
            meta.setObjectPath(objectName);
            meta.setProfileId(profileId);
            meta.setFilename(filename);
            meta.setBucketName(System.getenv().getOrDefault("GCS_BUCKET", "smart-bachat-dev-1"));
            meta.setStatus("PENDING");
            meta.setCreatedAt(Instant.now());
            meta.setUpdatedAt(Instant.now());
            metadataRepository.save(meta);

        // publish to pubsub
        JobMessage msg = new JobMessage(jobId.toString(), objectName, profileId.toString());
        String data = gson.toJson(msg);
        ByteString b = ByteString.copyFromUtf8(data);
        PubsubMessage pm = PubsubMessage.newBuilder().setData(b).build();
        ApiFuture<String> future = publisher.publish(pm);
        // optional: wait for publish
        future.get(5, TimeUnit.SECONDS);

        return new StartResponseDto(jobId);
    }

    public JobStatusDto getStatus(UUID jobId) {
        StatementMetadata meta = metadataRepository.findById(jobId).orElse(null);
        if (meta == null) return new JobStatusDto(jobId.toString(), "NOT_FOUND", null);
        return new JobStatusDto(meta.getId().toString(), meta.getStatus(), meta.getErrorMessage());
    }

    public void submitPassword(UUID jobId, String password) {
        // For dev: store password plaintext *only* for quick PoC. In prod: store in Secret Manager or pass directly to worker.
        StatementMetadata meta = metadataRepository.findById(jobId).orElseThrow();
        // Here we store password as errorMessage temporarily for demo (DO NOT DO IN PRODUCTION)
        meta.setErrorMessage("PASSWORD:" + password);
        meta.setStatus("PASSWORD_SUBMITTED");
        meta.setUpdatedAt(Instant.now());
        metadataRepository.save(meta);
    }

    public static class JobMessage {
        public String jobId;
        public String objectName;
        public String profileId;
        public JobMessage(String jobId, String objectName, String profileId) {
            this.jobId = jobId; this.objectName = objectName; this.profileId = profileId;
        }
    }

    public static class JobStatusDto {
        private final String jobId;
        private final String status;
        private final String error;
        public JobStatusDto(String jobId, String status, String error) { this.jobId = jobId; this.status = status; this.error = error;}
        public String getJobId() { return jobId; }
        public String getStatus() { return status; }
        public String getError() { return error; }
    }
}
