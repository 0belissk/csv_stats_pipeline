package com.paul.csvpipeline.backend.csvupload.service;

import com.paul.csvpipeline.backend.csvupload.CsvUploadRepository;
import com.paul.csvpipeline.backend.csvupload.CsvUploadResponse;
import com.paul.csvpipeline.backend.csvupload.UploadMetrics;
import com.paul.csvpipeline.backend.csvupload.entity.CsvUpload;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class CsvUploadService {

    private final CsvUploadRepository repository;
    private final UploadMetrics metrics;
    private final S3StorageService storage;

    public CsvUploadService(CsvUploadRepository repository,
                            UploadMetrics metrics,
                            S3StorageService storage) {
        this.repository = repository;
        this.metrics = metrics;
        this.storage = storage;
    }

    public CsvUploadResponse registerUpload(MultipartFile file, String userEmail) {
        metrics.markRequested();

        if (file == null || file.isEmpty()) {
            metrics.markRejected();
            throw new IllegalArgumentException("File is required");
        }

        if (!isCsv(file.getContentType(), file.getOriginalFilename())) {
            metrics.markRejected();
            throw new IllegalArgumentException("Only CSV uploads are supported");
        }

        String cleanedFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename());

        // Persist first to obtain uploadId
        CsvUpload upload = new CsvUpload(
                userEmail,
                cleanedFilename,
                file.getSize(),
                file.getContentType() == null ? "text/csv" : file.getContentType(),
                ""
        );
        CsvUpload saved = repository.save(upload);

        // Build required key pattern with persisted id
        String s3Key = "uploads/%s/%s/%s".formatted(userEmail, saved.getId(), cleanedFilename);

        // Upload to S3 (LocalStack in local/dev/test via endpoint override)
        storage.upload(s3Key, file);

        // Update entity with final key
        saved.setS3Key(s3Key);
        CsvUpload persisted = repository.save(saved);

        metrics.markAccepted();
        return CsvUploadResponse.from(persisted);
    }

    public List<CsvUploadResponse> listUploads(String userEmail) {
        metrics.markStatusPolled();
        return repository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(CsvUploadResponse::from)
                .toList();
    }

    public CsvUploadResponse getUpload(Long id, String userEmail) {
        metrics.markStatusPolled();
        CsvUpload upload = repository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Upload not found"));
        return CsvUploadResponse.from(upload);
    }

    private boolean isCsv(String contentType, String filename) {
        boolean contentTypeCsv = contentType != null &&
                (contentType.equalsIgnoreCase("text/csv") ||
                        contentType.equalsIgnoreCase("application/vnd.ms-excel"));
        boolean filenameCsv = filename != null && filename.toLowerCase().endsWith(".csv");
        return contentTypeCsv || filenameCsv;
    }
}