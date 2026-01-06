package com.paul.csvpipeline.backend.csvupload;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class CsvUploadService {

    private final CsvUploadRepository repository;
    private final UploadMetrics metrics;

    public CsvUploadService(CsvUploadRepository repository, UploadMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
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

        String cleanedFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename());
        String s3Key = buildKey(userEmail, cleanedFilename);

        CsvUpload upload = new CsvUpload(
                userEmail,
                cleanedFilename,
                file.getSize(),
                file.getContentType() == null ? "text/csv" : file.getContentType(),
                s3Key
        );

        CsvUpload saved = repository.save(upload);
        metrics.markAccepted();
        return CsvUploadResponse.from(saved);
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

    private String buildKey(String userEmail, String filename) {
        return "uploads/%s/%s/%s".formatted(
                userEmail,
                UUID.randomUUID(),
                filename
        );
    }
}
