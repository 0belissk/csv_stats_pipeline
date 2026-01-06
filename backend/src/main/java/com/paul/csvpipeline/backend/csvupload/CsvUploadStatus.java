package com.paul.csvpipeline.backend.csvupload;

public enum CsvUploadStatus {
    PENDING,
    VALIDATING,
    VALIDATED,
    VALIDATION_FAILED
}
