package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.UploadResponseDto;
import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class GcsUploadService {

    private final Storage storage;
    private final String bucketName;
    private final Integer ttlMinutes;

    public GcsUploadService(Storage storage,
                            @Value("${gcp.storage.bucket-name}") String bucketName,
                            @Value("${app.presign-url-ttl-minutes:30}") Integer ttlMinutes) {
        this.storage = storage;
        this.bucketName = bucketName;
        this.ttlMinutes = ttlMinutes;
    }

    public UploadResponseDto uploadFileAndCreateSignedUrl(MultipartFile file, String filename, String profileId) throws IOException {
        // Upload the MultipartFile to GCS
        Blob blob = storage.create(
                BlobInfo.newBuilder(bucketName, filename).build(),
                file.getBytes()
        );
        // Generate signed URL
        return createSignedUrl(filename, profileId);
    }


    public UploadResponseDto createSignedUrl(String filename, String profileId) {
        String uploadId = UUID.randomUUID().toString();
        String objectName = "uploads/" + profileId + "/" + uploadId + "/" + filename;

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName).build();

        URL signedUrl = storage.signUrl(blobInfo, ttlMinutes, TimeUnit.MINUTES, Storage.SignUrlOption.httpMethod(HttpMethod.PUT), Storage.SignUrlOption.withV4Signature());

        return new UploadResponseDto(uploadId, signedUrl.toString(), objectName);
    }
}
