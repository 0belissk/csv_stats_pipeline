package com.paul.csvpipeline.backend.csvupload.entity;

import com.paul.csvpipeline.backend.csvupload.CsvUploadStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "csv_uploads",
        indexes = {
                @Index(name = "idx_csv_uploads_user_email_created_at", columnList = "user_email,created_at DESC")
        }
)
public class CsvUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CsvUploadStatus status = CsvUploadStatus.PENDING;

    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CsvUpload() {
    }

    public CsvUpload(String userEmail, String filename, long sizeBytes, String contentType, String s3Key) {
        this.userEmail = userEmail;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.s3Key = (s3Key == null || s3Key.isBlank())
                ? "uploads/" + userEmail + "/" + UUID.randomUUID() + "/" + filename
                : s3Key;
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
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

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
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
