package com.paul.csvpipeline.backend.lambda;

import com.paul.csvpipeline.backend.IntegrationTestBase;
import com.paul.csvpipeline.backend.csvupload.CsvUploadStatus;
import com.paul.csvpipeline.backend.csvupload.entity.CsvUpload;
import com.paul.csvpipeline.backend.csvupload.repository.CsvUploadRepository;
import com.paul.csvpipeline.backend.lambda.handler.UploadStatusLambda;
import com.paul.csvpipeline.backend.lambda.persistence.UploadStatusRepository;
import com.paul.csvpipeline.backend.lambda.validation.ValidationError;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadStatusLambdaIT extends IntegrationTestBase {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Autowired
    private CsvUploadRepository repository;

    private UploadStatusRepository statusRepository;

    @BeforeAll
    void initRepo() {
        this.statusRepository = new UploadStatusRepository(jdbcUrl, username, password);
    }

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void markValidatedPersistsStatus() {
        CsvUpload upload = seedUpload("user@example.com", "valid.csv");
        UploadStatusLambda lambda = new UploadStatusLambda(statusRepository);

        lambda.handleRequest(new UploadStatusLambda.StatusRequest(upload.getId(), "VALIDATED", List.of()), null);

        CsvUpload refreshed = repository.findById(upload.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(CsvUploadStatus.VALIDATED);
        assertThat(refreshed.getErrorMessage()).isNull();
    }

    @Test
    void markFailedStoresErrors() {
        CsvUpload upload = seedUpload("user@example.com", "invalid.csv");
        UploadStatusLambda lambda = new UploadStatusLambda(statusRepository);

        lambda.handleRequest(new UploadStatusLambda.StatusRequest(
                upload.getId(),
                "VALIDATION_FAILED",
                List.of(new ValidationError(1, "amount", "Invalid decimal"))
        ), null);

        CsvUpload refreshed = repository.findById(upload.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(CsvUploadStatus.VALIDATION_FAILED);
        assertThat(refreshed.getErrorMessage()).contains("amount");
    }

    @Test
    void markValidatingMovesToInProgress() {
        CsvUpload upload = seedUpload("user@example.com", "pending.csv");
        UploadStatusLambda lambda = new UploadStatusLambda(statusRepository);

        lambda.handleRequest(new UploadStatusLambda.StatusRequest(upload.getId(), "VALIDATING", null), null);

        CsvUpload refreshed = repository.findById(upload.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(CsvUploadStatus.VALIDATING);
    }

    private CsvUpload seedUpload(String email, String filename) {
        CsvUpload upload = new CsvUpload(email, filename, 100, "text/csv", "");
        CsvUpload saved = repository.save(upload);
        saved.setS3Key("uploads/%s/%s/%s".formatted(email, saved.getId(), filename));
        return repository.save(saved);
    }
}