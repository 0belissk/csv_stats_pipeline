package com.paul.csvpipeline.backend.csvupload.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
public class S3StorageService {

    private final S3Client s3;
    private final String bucket;

    S3StorageService(S3Client s3, @Value("${csvpipeline.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    void upload(String key, MultipartFile file) {
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file bytes", e);
        }
    }
}
