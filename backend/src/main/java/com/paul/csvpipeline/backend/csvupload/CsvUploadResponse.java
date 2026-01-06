package com.paul.csvpipeline.backend.csvupload;

import java.time.Instant;

public record CsvUploadResponse(
        Long id,
        String filename,
        CsvUploadStatus status,
        String s3Key,
        Instant createdAt,
        Instant updatedAt
) {
    public static CsvUploadResponse from(CsvUpload upload) {
        return new CsvUploadResponse(
                upload.getId(),
                upload.getFilename(),
                upload.getStatus(),
                upload.getS3Key(),
                upload.getCreatedAt(),
                upload.getUpdatedAt()
        );
    }
}
