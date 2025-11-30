package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.StartRequestDto;
import com.ametsa.smartbachat.dto.StartResponseDto;
import com.ametsa.smartbachat.dto.UploadRequestDto;
import com.ametsa.smartbachat.dto.UploadResponseDto;

import com.ametsa.smartbachat.service.GcsUploadService;
import com.ametsa.smartbachat.service.JobService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingest")
public class PdfUploadController {

    private final GcsUploadService gcsUploadService;
    private final JobService jobService;

    public PdfUploadController(GcsUploadService gcsUploadService, JobService jobService) {
        this.gcsUploadService = gcsUploadService;
        this.jobService = jobService;
    }

    @PostMapping(value = "/request-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDto> requestUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("profileId") String profileId
    ) throws IOException {
        // Use original filename or generate unique name
        String filename = file.getOriginalFilename();

        // Call your GCS service to upload the file and return a signed URL
        UploadResponseDto resp = gcsUploadService.uploadFileAndCreateSignedUrl(file, filename, profileId);

        return ResponseEntity.ok(resp);
    }



    @PostMapping("/start")
    public ResponseEntity<StartResponseDto> startIngest(@Validated @RequestBody StartRequestDto req) throws Exception {
        StartResponseDto resp = jobService.startJob(req.getUploadId(), req.getObjectName(), req.getProfileId(), req.getFilename());
        return ResponseEntity.accepted().body(resp);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobService.JobStatusDto> status(@PathVariable UUID jobId) {
        JobService.JobStatusDto s = jobService.getStatus(jobId);
        return ResponseEntity.ok(s);
    }

    @PostMapping("/{jobId}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable UUID jobId, @RequestBody String password) throws Exception {
        jobService.submitPassword(jobId, password);
        return ResponseEntity.accepted().build();
    }
}
