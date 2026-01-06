package com.paul.csvpipeline.backend.csvupload;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "csv_uploads")
public class CsvUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CsvUploadStatus status = CsvUploadStatus.PENDING;

    @Column(nullable = false, length = 512)
    private String s3Key;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String contentType;

    @Column
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CsvUpload() {
    }

    public CsvUpload(String userEmail, String filename, long sizeBytes, String contentType, String s3Key) {
        this.userEmail = userEmail;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.s3Key = s3Key;
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.s3Key == null || this.s3Key.isBlank()) {
            this.s3Key = "uploads/" + userEmail + "/" + UUID.randomUUID() + "/" + filename;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getFilename() {
        return filename;
    }

    public CsvUploadStatus getStatus() {
        return status;
    }

    public void setStatus(CsvUploadStatus status) {
        this.status = status;
    }

    public String getS3Key() {
        return s3Key;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
