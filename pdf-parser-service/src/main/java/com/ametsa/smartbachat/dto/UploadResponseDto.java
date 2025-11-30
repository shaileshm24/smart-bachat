package com.ametsa.smartbachat.dto;

public class UploadResponseDto {
    private String uploadId;
    private String uploadUrl;
    private String objectName;

    public UploadResponseDto() {}
    public UploadResponseDto(String uploadId, String uploadUrl, String objectName) {
        this.uploadId = uploadId;
        this.uploadUrl = uploadUrl;
        this.objectName = objectName;
    }

    public UploadResponseDto(UploadResponseDto url) {
    }

    public String getUploadId() { return uploadId; }
    public String getUploadUrl() { return uploadUrl; }
    public String getObjectName() { return objectName; }
}
